package com.hatice.loginsight.dto;

public class ErrorFrequency {

    private String message;
    private String normalizedMessage;
    private int count;

    public ErrorFrequency() {
    }

    public ErrorFrequency(String message, int count) {
        this.message = message;
        this.count = count;
    }

    public ErrorFrequency(String message, String normalizedMessage, int count) {
        this.message = message;
        this.normalizedMessage = normalizedMessage;
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNormalizedMessage() {
        return normalizedMessage;
    }

    public void setNormalizedMessage(String normalizedMessage) {
        this.normalizedMessage = normalizedMessage;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}