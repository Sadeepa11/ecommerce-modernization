package com.techmart.jms;

import com.techmart.ejb.PlatformMetricsRegistry;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Queue;

@Stateless
public class OrderProcessingProducer {

    @Inject
    private JMSContext jmsContext;

    // Standard JNDI name for a queue in WildFly / GlassFish
    @Resource(lookup = "java:/jms/queue/OrderQueue")
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
