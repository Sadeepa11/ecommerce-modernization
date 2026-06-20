package com.techmart.jms;

import com.techmart.ejb.InventoryService;
import com.techmart.ejb.NotificationService;
import com.techmart.ejb.PlatformMetricsRegistry;
import com.techmart.model.OrderEntity;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

@JMSDestinationDefinition(
    name = "java:app/jms/OrderQueue",
    interfaceName = "jakarta.jms.Queue",
    destinationName = "OrderQueue"
)
@MessageDriven(name = "OrderProcessorMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:app/jms/OrderQueue"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class OrderProcessorMDB implements MessageListener {

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @EJB
    private InventoryService inventoryService;

    @EJB
    private NotificationService notificationService;

    @EJB
    private PlatformMetricsRegistry metrics;

    @Override
    public void onMessage(Message message) {
        long startTime = System.currentTimeMillis();
        metrics.incrementOrders(); // Count order received for processing

        try {
            if (message instanceof MapMessage) {
                MapMessage mapMsg = (MapMessage) message;
                String customerName = mapMsg.getString("customerName");
                String sku = mapMsg.getString("sku");
                int quantity = mapMsg.getInt("quantity");
                double price = mapMsg.getDouble("price");

                metrics.addLog("MDB: Received order request for customer '" + customerName + "', SKU '" + sku + "' x" + quantity);

                // Check and deduct inventory
                boolean success = inventoryService.deductStock(sku, quantity);
                String status;
                double total = price * quantity;

                if (success) {
                    status = "COMPLETED";
                    metrics.incrementSuccessfulOrders();
                    metrics.addLog("MDB: Order processed successfully for " + customerName);
                    
                    // Asynchronously send order success notification
                    notificationService.sendNotificationAsync(customerName, "Your order for " + quantity + "x " + sku + " was successful!");
                } else {
                    status = "FAILED";
                    metrics.incrementFailedOrders();
                    metrics.addLog("MDB: Order processing FAILED for " + customerName + " due to out-of-stock.");

                    // Asynchronously send order failure notification
                    notificationService.sendNotificationAsync(customerName, "Sorry! Your order for " + quantity + "x " + sku + " failed due to insufficient stock.");
                }

                // Create Order record in database
                OrderEntity order = new OrderEntity(customerName, sku, quantity, total, status);
                long duration = System.currentTimeMillis() - startTime;
                order.setProcessingTimeMs(duration);
                em.persist(order);

                // Register response latency
                metrics.addProcessingTime(duration);
                metrics.addLog("MDB: Order persisted in database. DB log ID: " + order.getId() + " (Time: " + duration + " ms)");
            } else {
                metrics.addLog("MDB: Received invalid message type.");
                metrics.incrementFailedOrders();
            }
        } catch (Exception e) {
            metrics.addLog("MDB: Exception occurred in message consumer: " + e.getMessage());
            metrics.incrementFailedOrders();
        }
    }
}
