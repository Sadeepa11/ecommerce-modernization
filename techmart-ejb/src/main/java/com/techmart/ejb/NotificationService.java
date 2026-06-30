package com.techmart.ejb;

import com.techmart.interceptor.PerformanceInterceptor;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import java.util.concurrent.Future;

@Stateless
@Interceptors(PerformanceInterceptor.class)
public class NotificationService {

    @EJB
    private PlatformMetricsRegistry metrics;

    @Asynchronous
    public Future<Boolean> sendNotificationAsync(String customerName, String message) {
        long startTime = System.currentTimeMillis();
        metrics.addLog("Starting asynchronous notification task for " + customerName);

        try {
            // Simulate notification delivery latency (e.g., mail server network delay)
            Thread.sleep(1500);
            metrics.addLog("Notification sent to " + customerName + ": '" + message + "'");
            long duration = System.currentTimeMillis() - startTime;
            metrics.addLog("Asynchronous task completed in " + duration + " ms.");
            return new AsyncResult<>(true);
        } catch (InterruptedException e) {
            metrics.addLog("Notification task interrupted for " + customerName);
            Thread.currentThread().interrupt();
            return new AsyncResult<>(false);
        } catch (Exception e) {
            metrics.addLog("Error sending asynchronous notification: " + e.getMessage());
            return new AsyncResult<>(false);
        }
    }
}
