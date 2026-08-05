package com.hatice.loginsight.dto;

public class ThreadFrequency {

    private String threadName;
    private long count;

    public ThreadFrequency() {
    }

    public ThreadFrequency(String threadName, long count) {
        this.threadName = threadName;
        this.count = count;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}