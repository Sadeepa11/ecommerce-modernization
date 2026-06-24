package com.techmart.jms;

import com.techmart.ejb.InventoryService;
import com.techmart.ejb.NotificationService;
import com.techmart.ejb.PlatformMetricsRegistry;
import com.techmart.model.OrderEntity;
import com.techmart.model.OrderItemEntity;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
                String itemsJson = mapMsg.getString("itemsJson");

                metrics.addLog("MDB: Received order request for customer '" + customerName + "'");

                // Parse items JSON
                JsonReader jsonReader = Json.createReader(new StringReader(itemsJson));
                JsonArray itemsArray = jsonReader.readArray();
                jsonReader.close();

                // Prepare stock deduction map
                Map<String, Integer> itemsToDeduct = new HashMap<>();
                for (int i = 0; i < itemsArray.size(); i++) {
                    JsonObject obj = itemsArray.getJsonObject(i);
                    itemsToDeduct.put(obj.getString("sku"), obj.getInt("quantity"));
                }

                // Check and deduct inventory atomically
                boolean success = inventoryService.deductStockForOrder(itemsToDeduct);
                String status;

                if (success) {
                    status = "COMPLETED";
                    metrics.incrementSuccessfulOrders();
                    metrics.addLog("MDB: Order processed successfully for " + customerName);
                    
                    // Asynchronously send order success notification
                    StringBuilder notificationMsg = new StringBuilder("Your order was successful! Items: ");
                    for (int i = 0; i < itemsArray.size(); i++) {
                        JsonObject obj = itemsArray.getJsonObject(i);
                        notificationMsg.append(obj.getInt("quantity")).append("x ").append(obj.getString("sku"));
                        if (i < itemsArray.size() - 1) {
                            notificationMsg.append(", ");
                        }
                    }
                    notificationService.sendNotificationAsync(customerName, notificationMsg.toString());
                } else {
                    status = "FAILED";
                    metrics.incrementFailedOrders();
                    metrics.addLog("MDB: Order processing FAILED for " + customerName + " due to out-of-stock.");

                    // Asynchronously send order failure notification
                    StringBuilder notificationMsg = new StringBuilder("Sorry! Your order failed due to insufficient stock. Items requested: ");
                    for (int i = 0; i < itemsArray.size(); i++) {
                        JsonObject obj = itemsArray.getJsonObject(i);
                        notificationMsg.append(obj.getInt("quantity")).append("x ").append(obj.getString("sku"));
                        if (i < itemsArray.size() - 1) {
                            notificationMsg.append(", ");
                        }
                    }
                    notificationService.sendNotificationAsync(customerName, notificationMsg.toString());
                }

                // Create Order record in database
                OrderEntity order = new OrderEntity();
                order.setCustomerName(customerName);
                order.setStatus(status);
                order.setCreatedAt(LocalDateTime.now());

                double total = 0.0;
                List<OrderItemEntity> orderItems = new ArrayList<>();
                for (int i = 0; i < itemsArray.size(); i++) {
                    JsonObject obj = itemsArray.getJsonObject(i);
                    String sku = obj.getString("sku");
                    int quantity = obj.getInt("quantity");
                    double price = obj.getJsonNumber("price").doubleValue();
                    total += price * quantity;

                    OrderItemEntity item = new OrderItemEntity(order, sku, quantity, price);
                    orderItems.add(item);
                }
                order.setTotalPrice(total);
                order.setItems(orderItems);

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
