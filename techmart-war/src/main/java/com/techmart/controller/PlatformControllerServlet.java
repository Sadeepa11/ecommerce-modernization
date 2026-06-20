package com.techmart.controller;

import com.techmart.ejb.InventoryService;
import com.techmart.ejb.PlatformMetricsRegistry;
import com.techmart.ejb.ShoppingCartSession;
import com.techmart.jms.OrderProcessingProducer;
import com.techmart.model.Product;
import jakarta.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PlatformControllerServlet", urlPatterns = {
    "/api/products",
    "/api/cart/add",
    "/api/cart/view",
    "/api/checkout",
    "/api/metrics",
    "/api/seed"
})
public class PlatformControllerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Use Dependency Injection (@EJB) for Stateless and Singleton EJBs
    @EJB
    private InventoryService inventoryService;

    @EJB
    private PlatformMetricsRegistry metricsRegistry;

    @EJB
    private OrderProcessingProducer orderProducer;

    // Retrieve Stateful EJB instance from Session or JNDI Lookup
    private ShoppingCartSession getOrCreateCartSession(HttpServletRequest request) throws NamingException {
        HttpSession httpSession = request.getSession(true);
        ShoppingCartSession cart = (ShoppingCartSession) httpSession.getAttribute("cart_ejb");
        if (cart == null) {
            // Demonstrating JNDI Lookup for EJB component lookup as requested in assignment
            InitialContext ic = new InitialContext();
            // Standard portable EJB JNDI name
            cart = (ShoppingCartSession) ic.lookup("java:app/techmart-ejb/ShoppingCartSession");
            httpSession.setAttribute("cart_ejb", cart);
        }
        return cart;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        metricsRegistry.incrementRequests();
        String path = request.getServletPath();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            if ("/api/products".equals(path)) {
                List<Product> products = inventoryService.getAllProducts();
                out.print(toJsonProducts(products));
            } else if ("/api/metrics".equals(path)) {
                out.print(toJsonMetrics());
            } else if ("/api/cart/view".equals(path)) {
                ShoppingCartSession cart = getOrCreateCartSession(request);
                out.print(toJsonCart(cart));
            } else if ("/api/seed".equals(path)) {
                seedDatabase();
                out.print("{\"status\":\"success\", \"message\":\"Database seeded with sample products.\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Invalid endpoint\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        metricsRegistry.incrementRequests();
        String path = request.getServletPath();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            if ("/api/cart/add".equals(path)) {
                String sku = request.getParameter("sku");
                int qty = Integer.parseInt(request.getParameter("quantity"));
                String customerName = request.getParameter("customerName");

                if (sku == null || qty <= 0 || customerName == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Missing parameters sku, quantity, or customerName\"}");
                    return;
                }

                ShoppingCartSession cart = getOrCreateCartSession(request);
                cart.setCustomerName(customerName);
                
                // Verify availability first (using Stateless session bean)
                boolean available = inventoryService.checkAvailability(sku, qty);
                if (available) {
                    cart.addItem(sku, qty);
                    out.print("{\"status\":\"success\", \"message\":\"Added to cart\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print("{\"status\":\"failed\", \"message\":\"Insufficient stock available\"}");
                }

            } else if ("/api/checkout".equals(path)) {
                ShoppingCartSession cart = getOrCreateCartSession(request);
                Map<String, Integer> items = cart.getCartItems();
                String customer = cart.getCustomerName();

                if (items.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Cart is empty\"}");
                    return;
                }

                // Process each cart item through JMS Queue asynchronously
                for (Map.Entry<String, Integer> entry : items.entrySet()) {
                    Product product = inventoryService.getProductBySku(entry.getKey());
                    double price = product != null ? product.getPrice() : 0.0;
                    orderProducer.sendOrderMessage(customer, entry.getKey(), entry.getValue(), price);
                }

                cart.clearCart();
                out.print("{\"status\":\"success\", \"message\":\"Order placed successfully in JMS queue! Processing asynchronously.\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Invalid endpoint\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void seedDatabase() {
        if (inventoryService.getAllProducts().isEmpty()) {
            inventoryService.createProduct(new Product("LAP-01", "Developer Enterprise Laptop", 50, 1200.00, "Warehouse A"));
            inventoryService.createProduct(new Product("PHN-02", "Smart Mobile Phone Pro", 120, 800.00, "Warehouse B"));
            inventoryService.createProduct(new Product("KEY-03", "Mechanical RGB Keyboard", 200, 150.00, "Warehouse A"));
            inventoryService.createProduct(new Product("MOU-04", "Wireless Ergonomic Mouse", 150, 75.00, "Warehouse C"));
            metricsRegistry.addLog("Database seeded with sample product records.");
        }
    }

    // JSON Helper formatters
    private String toJsonProducts(List<Product> products) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            json.append(String.format("{\"id\":%d,\"sku\":\"%s\",\"name\":\"%s\",\"stock\":%d,\"price\":%.2f,\"warehouse\":\"%s\"}",
                    p.getId(), p.getSku(), p.getName(), p.getStock(), p.getPrice(), p.getWarehouseLocation()));
            if (i < products.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private String toJsonCart(ShoppingCartSession cart) {
        StringBuilder json = new StringBuilder("{\"customerName\":\"" + cart.getCustomerName() + "\",\"items\":{");
        Map<String, Integer> items = cart.getCartItems();
        int count = 0;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            json.append(String.format("\"%s\":%d", entry.getKey(), entry.getValue()));
            if (++count < items.size()) json.append(",");
        }
        json.append("}}");
        return json.toString();
    }

    private String toJsonMetrics() {
        StringBuilder logsJson = new StringBuilder("[");
        List<String> logs = metricsRegistry.getSystemLogs();
        for (int i = 0; i < logs.size(); i++) {
            logsJson.append("\"").append(logs.get(i).replace("\"", "\\\"")).append("\"");
            if (i < logs.size() - 1) logsJson.append(",");
        }
        logsJson.append("]");

        return String.format("{\"totalRequests\":%d,\"totalOrders\":%d,\"successfulOrders\":%d,\"failedOrders\":%d,\"avgResponseTimeMs\":%d,\"activeSessions\":%d,\"logs\":%s}",
                metricsRegistry.getTotalRequests(),
                metricsRegistry.getTotalOrders(),
                metricsRegistry.getSuccessfulOrders(),
                metricsRegistry.getFailedOrders(),
                metricsRegistry.getAverageResponseTimeMs(),
                metricsRegistry.getActiveSessions(),
                logsJson.toString());
    }
}
