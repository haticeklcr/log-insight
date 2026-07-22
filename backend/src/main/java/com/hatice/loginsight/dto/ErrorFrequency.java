package com.hatice.loginsight.dto;

public class ErrorFrequency {

    private String message;
    private int count;

    public ErrorFrequency() {
    }

    public ErrorFrequency(String message, int count) {
        this.message = message;
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}