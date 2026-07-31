package com.hatice.loginsight.dto;

public class ThreadFrequency {

    private String threadName;
    private int count;

    public ThreadFrequency() {
    }

    public ThreadFrequency(String threadName, int count) {
        this.threadName = threadName;
        this.count = count;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}