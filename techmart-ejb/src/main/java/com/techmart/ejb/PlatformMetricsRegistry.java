package com.techmart.ejb;

import com.techmart.model.BeanCallRecord;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.sql.DataSourceDefinition;

@DataSourceDefinition(
    name = "java:app/jdbc/TechMartDS",
    className = "com.mysql.cj.jdbc.MysqlDataSource",
    url = "jdbc:mysql://localhost:3306/techmart_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true",
    user = "root",
    password = "Sata.Pata.123"
)
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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

    // Performance metrics fields
    private List<BeanCallRecord> beanCalls;
    private AtomicLong totalBeanCalls;
    private AtomicLong stockCheckCount;
    private AtomicLong stockCheckTotalTimeMs;
    private AtomicLong cartUpdateCount;
    private AtomicLong cartUpdateTotalTimeMs;
    private AtomicLong orderPlacementCount;
    private AtomicLong orderPlacementTotalTimeMs;

    @PostConstruct
    public void init() {
        totalRequests = new AtomicLong(0);
        totalOrders = new AtomicLong(0);
        successfulOrders = new AtomicLong(0);
        failedOrders = new AtomicLong(0);
        totalProcessingTimeMs = new AtomicLong(0);
        activeSessions = new AtomicInteger(0);
        systemLogs = new CopyOnWriteArrayList<>();
        
        beanCalls = new CopyOnWriteArrayList<>();
        totalBeanCalls = new AtomicLong(0);
        stockCheckCount = new AtomicLong(0);
        stockCheckTotalTimeMs = new AtomicLong(0);
        cartUpdateCount = new AtomicLong(0);
        cartUpdateTotalTimeMs = new AtomicLong(0);
        orderPlacementCount = new AtomicLong(0);
        orderPlacementTotalTimeMs = new AtomicLong(0);
        
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

    @Lock(LockType.WRITE)
    public void recordBeanCall(String methodName, String ejbType, long durationMs, String status) {
        totalBeanCalls.incrementAndGet();
        BeanCallRecord record = new BeanCallRecord(methodName, ejbType, durationMs, status, System.currentTimeMillis());
        beanCalls.add(record);
        if (beanCalls.size() > 100) {
            beanCalls.remove(0);
        }

        // Categorize bean calls to calculate average response times
        String lowerMethod = methodName.toLowerCase();
        if (lowerMethod.contains("checkavailability") || lowerMethod.contains("getproduct")) {
            stockCheckCount.incrementAndGet();
            stockCheckTotalTimeMs.addAndGet(durationMs);
        } else if (lowerMethod.contains("additem") || lowerMethod.contains("removeitem") || lowerMethod.contains("clearcart") || lowerMethod.contains("getcartitems")) {
            cartUpdateCount.incrementAndGet();
            cartUpdateTotalTimeMs.addAndGet(durationMs);
        } else if (lowerMethod.contains("checkout") || lowerMethod.contains("sendordermessage") || lowerMethod.contains("onmessage")) {
            orderPlacementCount.incrementAndGet();
            orderPlacementTotalTimeMs.addAndGet(durationMs);
        }
    }

    @Lock(LockType.WRITE)
    public void resetMetrics() {
        totalRequests.set(0);
        totalOrders.set(0);
        successfulOrders.set(0);
        failedOrders.set(0);
        totalProcessingTimeMs.set(0);
        
        beanCalls.clear();
        totalBeanCalls.set(0);
        stockCheckCount.set(0);
        stockCheckTotalTimeMs.set(0);
        cartUpdateCount.set(0);
        cartUpdateTotalTimeMs.set(0);
        orderPlacementCount.set(0);
        orderPlacementTotalTimeMs.set(0);
        
        systemLogs.clear();
        addLog("Platform Metrics Reset successfully.");
    }

    @Lock(LockType.READ)
    public List<BeanCallRecord> getBeanCalls() {
        return new ArrayList<>(beanCalls);
    }

    @Lock(LockType.READ)
    public long getTotalBeanCalls() {
        return totalBeanCalls.get();
    }

    @Lock(LockType.READ)
    public double getAvgStockCheckTimeMs() {
        long count = stockCheckCount.get();
        return count == 0 ? 0.0 : (double) stockCheckTotalTimeMs.get() / count;
    }

    @Lock(LockType.READ)
    public double getAvgCartUpdateTimeMs() {
        long count = cartUpdateCount.get();
        return count == 0 ? 0.0 : (double) cartUpdateTotalTimeMs.get() / count;
    }

    @Lock(LockType.READ)
    public double getAvgOrderPlacementTimeMs() {
        long count = orderPlacementCount.get();
        return count == 0 ? 0.0 : (double) orderPlacementTotalTimeMs.get() / count;
    }
}
