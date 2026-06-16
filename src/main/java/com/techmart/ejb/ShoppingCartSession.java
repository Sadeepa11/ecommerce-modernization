package com.techmart.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateful;
import javax.ejb.StatefulTimeout;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Stateful
@StatefulTimeout(value = 20, unit = TimeUnit.MINUTES)
public class ShoppingCartSession implements Serializable {
    private static final long serialVersionUID = 1L;

    @EJB
    private PlatformMetricsRegistry metrics;

    private Map<String, Integer> cartItems;
    private String customerName;

    @PostConstruct
    public void init() {
        cartItems = new HashMap<>();
        if (metrics != null) {
            metrics.incrementActiveSessions();
            metrics.addLog("Stateful ShoppingCartSession initialized.");
        }
    }

    @PreDestroy
    public void destroy() {
        if (metrics != null) {
            metrics.decrementActiveSessions();
            metrics.addLog("Stateful ShoppingCartSession destroyed/timed out.");
        }
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void addItem(String sku, int quantity) {
        cartItems.put(sku, cartItems.getOrDefault(sku, 0) + quantity);
        metrics.addLog("Item added to cart: " + sku + " (Qty: " + quantity + ") for customer " + customerName);
    }

    public void removeItem(String sku) {
        if (cartItems.containsKey(sku)) {
            cartItems.remove(sku);
            metrics.addLog("Item removed from cart: " + sku + " for customer " + customerName);
        }
    }

    public Map<String, Integer> getCartItems() {
        return new HashMap<>(cartItems);
    }

    public void clearCart() {
        cartItems.clear();
        metrics.addLog("Cart cleared for customer " + customerName);
    }
}
