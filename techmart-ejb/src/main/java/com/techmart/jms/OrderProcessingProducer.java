package com.techmart.jms;

import com.techmart.ejb.PlatformMetricsRegistry;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSProducer;
import jakarta.jms.MapMessage;
import jakarta.jms.Queue;

@JMSDestinationDefinition(
    name = "java:app/jms/OrderQueue",
    interfaceName = "jakarta.jms.Queue",
    destinationName = "OrderQueue"
)
@Stateless
public class OrderProcessingProducer {

    @Inject
    private JMSContext jmsContext;

    // Standard JNDI name for a queue in WildFly / GlassFish
    @Resource(lookup = "java:app/jms/OrderQueue")
    private Queue orderQueue;

    @EJB
    private PlatformMetricsRegistry metrics;

    public void sendOrderMessage(String customerName, String sku, int quantity, double price) {
        try {
            metrics.incrementRequests(); // Count this incoming request
            metrics.addLog("Creating JMS message for order: " + customerName + ", SKU: " + sku + " x" + quantity);

            MapMessage mapMessage = jmsContext.createMapMessage();
            mapMessage.setString("customerName", customerName);
            mapMessage.setString("sku", sku);
            mapMessage.setInt("quantity", quantity);
            mapMessage.setDouble("price", price);

            JMSProducer producer = jmsContext.createProducer();
            
            // Set message delivery guarantee options (e.g. Non-Persistent or Persistent)
            // To optimize performance, we can configure delivery options here
            producer.send(orderQueue, mapMessage);

            metrics.addLog("Order message sent to JMS Queue: java:/jms/queue/OrderQueue successfully.");
        } catch (Exception e) {
            metrics.addLog("JMS Error sending message: " + e.getMessage());
            metrics.incrementFailedOrders();
        }
    }
}
