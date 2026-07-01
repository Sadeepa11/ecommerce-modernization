package com.techmart.controller;

import com.techmart.ejb.InventoryService;
import com.techmart.ejb.PlatformMetricsRegistry;
import com.techmart.ejb.ShoppingCartSession;
import com.techmart.jms.OrderProcessingProducer;
import com.techmart.model.Product;
import com.techmart.model.OrderEntity;
import com.techmart.model.BeanCallRecord;
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
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

@WebServlet(name = "PlatformControllerServlet", urlPatterns = {
    "/api/products",
    "/api/cart/add",
    "/api/cart/view",
    "/api/checkout",
    "/api/metrics",
    "/api/seed",
    "/api/orders",
    "/api/orders/demo",
    "/api/metrics/reset",
    "/api/admin/products/update",
    "/api/admin/products/create"
})
public class PlatformControllerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    private InventoryService inventoryService;

    @EJB
    private PlatformMetricsRegistry metricsRegistry;

    @EJB
    private OrderProcessingProducer orderProducer;

    private ShoppingCartSession getOrCreateCartSession(HttpServletRequest request) throws NamingException {
        HttpSession httpSession = request.getSession(true);
        ShoppingCartSession cart = (ShoppingCartSession) httpSession.getAttribute("cart_ejb");
        if (cart == null) {
            InitialContext ic = new InitialContext();
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
            } else if ("/api/orders".equals(path)) {
                List<OrderEntity> orders = inventoryService.getAllOrders();
                out.print(toJsonOrders(orders));
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
                
                String customerName = request.getParameter("customerName");
                if (customerName != null && !customerName.trim().isEmpty()) {
                    cart.setCustomerName(customerName);
                }
                
                Map<String, Integer> items = cart.getCartItems();
                String customer = cart.getCustomerName();

                if (items.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Cart is empty\"}");
                    return;
                }

                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Map.Entry<String, Integer> entry : items.entrySet()) {
                    Product product = inventoryService.getProductBySku(entry.getKey());
                    double price = product != null ? product.getPrice() : 0.0;
                    JsonObjectBuilder item = Json.createObjectBuilder()
                        .add("sku", entry.getKey())
                        .add("quantity", entry.getValue())
                        .add("price", price);
                    arrayBuilder.add(item);
                }
                String itemsJson = arrayBuilder.build().toString();

                orderProducer.sendOrderMessage(customer, itemsJson);
                cart.clearCart();
                out.print("{\"status\":\"success\", \"message\":\"Order placed successfully in JMS queue! Processing asynchronously.\"}");

            } else if ("/api/orders/demo".equals(path)) {
                List<Product> products = inventoryService.getAllProducts();
                if (products.isEmpty()) {
                    seedDatabase();
                    products = inventoryService.getAllProducts();
                }

                if (products.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"error\":\"No products available to create demo order.\"}");
                    return;
                }

                // Pick a random product
                Product p = products.get((int) (Math.random() * products.size()));
                int qty = (int) (Math.random() * 3) + 1; // 1 to 3

                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                JsonObjectBuilder item = Json.createObjectBuilder()
                    .add("sku", p.getSku())
                    .add("quantity", qty)
                    .add("price", p.getPrice());
                arrayBuilder.add(item);

                String customer = "Demo Customer " + ((int)(Math.random() * 100) + 1);
                orderProducer.sendOrderMessage(customer, arrayBuilder.build().toString());

                out.print("{\"status\":\"success\", \"message\":\"Demo order for " + customer + " placed in JMS queue.\"}");

            } else if ("/api/metrics/reset".equals(path)) {
                metricsRegistry.resetMetrics();
                out.print("{\"status\":\"success\", \"message\":\"System metrics reset successfully.\"}");

            } else if ("/api/admin/products/update".equals(path)) {
                String sku = request.getParameter("sku");
                String name = request.getParameter("name");
                int stock = Integer.parseInt(request.getParameter("stock"));
                double price = Double.parseDouble(request.getParameter("price"));
                String warehouse = request.getParameter("warehouse");
                String category = request.getParameter("category");

                if (sku == null || name == null || warehouse == null || category == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Missing required parameters for product update\"}");
                    return;
                }

                inventoryService.updateProduct(sku, name, stock, price, warehouse, category);
                out.print("{\"status\":\"success\", \"message\":\"Product updated successfully.\"}");

            } else if ("/api/admin/products/create".equals(path)) {
                String sku = request.getParameter("sku");
                String name = request.getParameter("name");
                int stock = Integer.parseInt(request.getParameter("stock"));
                double price = Double.parseDouble(request.getParameter("price"));
                String warehouse = request.getParameter("warehouse");
                String category = request.getParameter("category");

                if (sku == null || name == null || warehouse == null || category == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Missing required parameters for product creation\"}");
                    return;
                }

                Product p = new Product(sku, name, stock, price, warehouse, category);
                inventoryService.createProduct(p);
                out.print("{\"status\":\"success\", \"message\":\"Product created successfully.\"}");

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
            inventoryService.createProduct(new Product("AUD-01", "Wireless headphones", 50, 79.99, "Warehouse A", "Audio"));
            inventoryService.createProduct(new Product("ACC-01", "Smart watch", 3, 149.00, "Warehouse B", "Accessories"));
            inventoryService.createProduct(new Product("ACC-02", "Wireless mouse", 100, 29.99, "Warehouse C", "Accessories"));
            inventoryService.createProduct(new Product("ACC-03", "Mechanical keyboard", 120, 89.50, "Warehouse A", "Accessories"));
            inventoryService.createProduct(new Product("AUD-02", "Bluetooth speaker", 0, 59.00, "Warehouse B", "Audio"));
            inventoryService.createProduct(new Product("ACC-04", "4K webcam", 80, 64.99, "Warehouse C", "Accessories"));
            metricsRegistry.addLog("Database seeded with sample product records.");
        }
    }

    private String toJsonProducts(List<Product> products) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            json.append(String.format("{\"id\":%d,\"sku\":\"%s\",\"name\":\"%s\",\"stock\":%d,\"price\":%.2f,\"warehouse\":\"%s\",\"category\":\"%s\"}",
                    p.getId(), p.getSku(), p.getName(), p.getStock(), p.getPrice(), p.getWarehouseLocation(), p.getCategory()));
            if (i < products.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private String toJsonCart(ShoppingCartSession cart) {
        StringBuilder json = new StringBuilder("{\"customerName\":\"" + (cart.getCustomerName() != null ? cart.getCustomerName() : "") + "\",\"items\":{");
        Map<String, Integer> items = cart.getCartItems();
        int count = 0;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            json.append(String.format("\"%s\":%d", entry.getKey(), entry.getValue()));
            if (++count < items.size()) json.append(",");
        }
        json.append("}}");
        return json.toString();
    }

    private String toJsonOrders(List<OrderEntity> orders) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < orders.size(); i++) {
            OrderEntity o = orders.get(i);
            String customerName = o.getCustomerName() != null ? o.getCustomerName().replace("\"", "\\\"") : "Guest";
            double totalPrice = o.getTotalPrice() != null ? o.getTotalPrice() : 0.0;
            String status = o.getStatus() != null ? o.getStatus() : "PENDING";
            long processingTimeMs = o.getProcessingTimeMs() != null ? o.getProcessingTimeMs() : 0L;
            String dateStr = o.getCreatedAt() != null ? o.getCreatedAt().toString() : "";
            
            json.append(String.format("{\"id\":%d,\"customerName\":\"%s\",\"totalPrice\":%.2f,\"status\":\"%s\",\"processingTimeMs\":%d,\"createdAt\":\"%s\"}",
                    o.getId(), customerName, totalPrice, status, processingTimeMs, dateStr));
            if (i < orders.size() - 1) json.append(",");
        }
        json.append("]");
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

        StringBuilder callsJson = new StringBuilder("[");
        List<BeanCallRecord> calls = metricsRegistry.getBeanCalls();
        for (int i = 0; i < calls.size(); i++) {
            BeanCallRecord c = calls.get(i);
            callsJson.append(String.format("{\"methodName\":\"%s\",\"ejbType\":\"%s\",\"durationMs\":%d,\"status\":\"%s\",\"timestamp\":%d}",
                    c.getMethodName(), c.getEjbType(), c.getDurationMs(), c.getStatus(), c.getTimestamp()));
            if (i < calls.size() - 1) callsJson.append(",");
        }
        callsJson.append("]");

        return String.format("{\"totalRequests\":%d,\"totalOrders\":%d,\"successfulOrders\":%d,\"failedOrders\":%d,\"avgResponseTimeMs\":%d,\"activeSessions\":%d,\"logs\":%s,\"totalBeanCalls\":%d,\"avgStockCheck\":%.2f,\"avgCartUpdate\":%.2f,\"avgOrderPlacement\":%.2f,\"beanCalls\":%s}",
                metricsRegistry.getTotalRequests(),
                metricsRegistry.getTotalOrders(),
                metricsRegistry.getSuccessfulOrders(),
                metricsRegistry.getFailedOrders(),
                metricsRegistry.getAverageResponseTimeMs(),
                metricsRegistry.getActiveSessions(),
                logsJson.toString(),
                metricsRegistry.getTotalBeanCalls(),
                metricsRegistry.getAvgStockCheckTimeMs(),
                metricsRegistry.getAvgCartUpdateTimeMs(),
                metricsRegistry.getAvgOrderPlacementTimeMs(),
                callsJson.toString());
    }
}
