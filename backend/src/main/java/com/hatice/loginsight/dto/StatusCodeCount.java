package com.hatice.loginsight.dto;

public class StatusCodeCount {

    private int statusCode;
    private int count;

    public StatusCodeCount() {
    }

    public StatusCodeCount(int statusCode, int count) {
        this.statusCode = statusCode;
        this.count = count;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}