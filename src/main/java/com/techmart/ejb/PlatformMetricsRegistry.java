package com.techmart.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@LocalBean
public class PlatformMetricsRegistry implements Serializable {
    private static final long serialVersionUID = 1L;

    private AtomicLong totalRequests;
    private AtomicLong totalOrders;
    private AtomicLong successfulOrders;
    private AtomicLong failedOrders;
    private AtomicLong totalProcessingTimeMs;
    private AtomicInteger activeSessions;
    private List<String> systemLogs;

    @PostConstruct
    public void init() {
        totalRequests = new AtomicLong(0);
        totalOrders = new AtomicLong(0);
        successfulOrders = new AtomicLong(0);
        failedOrders = new AtomicLong(0);
        totalProcessingTimeMs = new AtomicLong(0);
        activeSessions = new AtomicInteger(0);
        systemLogs = new CopyOnWriteArrayList<>();
        addLog("Platform Metrics Registry initialized successfully.");
    }

    @Lock(LockType.WRITE)
    public void addLog(String message) {
        String logEntry = "[" + java.time.LocalDateTime.now() + "] - " + message;
        systemLogs.add(logEntry);
        // Keep logs size bounded (e.g. last 30 logs)
        if (systemLogs.size() > 30) {
            systemLogs.remove(0);
        }
    }

    @Lock(LockType.READ)
    public List<String> getSystemLogs() {
        return new ArrayList<>(systemLogs);
    }

    @Lock(LockType.WRITE)
    public void incrementRequests() {
        totalRequests.incrementAndGet();
    }

    @Lock(LockType.WRITE)
    public void incrementOrders() {
        totalOrders.incrementAndGet();
    }

    @Lock(LockType.WRITE)
    public void incrementSuccessfulOrders() {
        successfulOrders.incrementAndGet();
    }

    @Lock(LockType.WRITE)
    public void incrementFailedOrders() {
        failedOrders.incrementAndGet();
    }

    @Lock(LockType.WRITE)
    public void addProcessingTime(long ms) {
        totalProcessingTimeMs.addAndGet(ms);
    }

    @Lock(LockType.WRITE)
    public void incrementActiveSessions() {
        activeSessions.incrementAndGet();
    }

    @Lock(LockType.WRITE)
    public void decrementActiveSessions() {
        if (activeSessions.get() > 0) {
            activeSessions.decrementAndGet();
        }
    }

    // Read locks for metric values
    @Lock(LockType.READ)
    public long getTotalRequests() {
        return totalRequests.get();
    }

    @Lock(LockType.READ)
    public long getTotalOrders() {
        return totalOrders.get();
    }

    @Lock(LockType.READ)
    public long getSuccessfulOrders() {
        return successfulOrders.get();
    }

    @Lock(LockType.READ)
    public long getFailedOrders() {
        return failedOrders.get();
    }

    @Lock(LockType.READ)
    public long getAverageResponseTimeMs() {
        long orders = totalOrders.get();
        return orders == 0 ? 0 : totalProcessingTimeMs.get() / orders;
    }

    @Lock(LockType.READ)
    public int getActiveSessions() {
        return activeSessions.get();
    }
}
