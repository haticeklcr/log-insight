package com.hatice.loginsight.dto;

public class HttpMethodCount {

    private String httpMethod;
    private int count;

    public HttpMethodCount() {
    }

    public HttpMethodCount(String httpMethod, int count) {
        this.httpMethod = httpMethod;
        this.count = count;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}