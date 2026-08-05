package com.hatice.loginsight.dto;

public class ErrorFrequency {

    private String message;
    private String normalizedMessage;
    private long count;

    public ErrorFrequency() {
    }

    public ErrorFrequency(String message, long count) {
        this.message = message;
        this.count = count;
    }

    public ErrorFrequency(String message, String normalizedMessage, long count) {
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

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}