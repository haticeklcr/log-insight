package com.hatice.loginsight.dto;

public class StatusCodeCount {

    private int statusCode;
    private long count;

    public StatusCodeCount() {
    }

    public StatusCodeCount(int statusCode, long count) {
        this.statusCode = statusCode;
        this.count = count;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}