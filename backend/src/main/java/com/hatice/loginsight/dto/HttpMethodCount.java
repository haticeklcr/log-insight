package com.hatice.loginsight.dto;

public class HttpMethodCount {

    private String httpMethod;
    private long count;

    public HttpMethodCount() {
    }

    public HttpMethodCount(String httpMethod, long count) {
        this.httpMethod = httpMethod;
        this.count = count;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}