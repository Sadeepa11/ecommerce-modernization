package com.techmart.model;

import java.io.Serializable;

public class BeanCallRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String methodName;
    private String ejbType;
    private long durationMs;
    private String status;
    private long timestamp;

    public BeanCallRecord() {}

    public BeanCallRecord(String methodName, String ejbType, long durationMs, String status, long timestamp) {
        this.methodName = methodName;
        this.ejbType = ejbType;
        this.durationMs = durationMs;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getEjbType() {
        return ejbType;
    }

    public void setEjbType(String ejbType) {
        this.ejbType = ejbType;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
