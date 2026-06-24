package com.techmart.ejb;

import com.techmart.model.Product;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

@Stateless
public class InventoryService {

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @EJB
    private PlatformMetricsRegistry metrics;

    public List<Product> getAllProducts() {
        TypedQuery<Product> query = em.createQuery("SELECT p FROM Product p", Product.class);
        return query.getResultList();
    }

    public Product getProductBySku(String sku) {
        try {
            TypedQuery<Product> query = em.createQuery("SELECT p FROM Product p WHERE p.sku = :sku", Product.class);
            query.setParameter("sku", sku);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean checkAvailability(String sku, int quantity) {
        Product product = getProductBySku(sku);
        return product != null && product.getStock() >= quantity;
    }

    public synchronized boolean deductStock(String sku, int quantity) {
        Product product = getProductBySku(sku);
        if (product != null && product.getStock() >= quantity) {
            product.setStock(product.getStock() - quantity);
            em.merge(product);
            metrics.addLog("Stock deducted for SKU: " + sku + " (Qty: " + quantity + "). New stock: " + product.getStock());
            return true;
        }
        metrics.addLog("Failed to deduct stock for SKU: " + sku + ". Insufficient inventory.");
        return false;
    }

    public synchronized boolean deductStockForOrder(java.util.Map<String, Integer> items) {
        // First check availability of all items to ensure atomicity
        for (java.util.Map.Entry<String, Integer> entry : items.entrySet()) {
            if (!checkAvailability(entry.getKey(), entry.getValue())) {
                metrics.addLog("Order Stock Deduction failed: SKU " + entry.getKey() + " has insufficient inventory.");
                return false;
            }
        }
        // If all are available, deduct all
        for (java.util.Map.Entry<String, Integer> entry : items.entrySet()) {
            deductStock(entry.getKey(), entry.getValue());
        }
        return true;
    }

    public void restock(String sku, int quantity) {
        Product product = getProductBySku(sku);
        if (product != null) {
            product.setStock(product.getStock() + quantity);
            em.merge(product);
            metrics.addLog("Stock restocked for SKU: " + sku + " (Qty: " + quantity + "). New stock: " + product.getStock());
        } else {
            metrics.addLog("Failed to restock SKU: " + sku + ". SKU not found.");
        }
    }

    public void createProduct(Product product) {
        em.persist(product);
        metrics.addLog("New product created: " + product.getName() + " (SKU: " + product.getSku() + ")");
    }
}
