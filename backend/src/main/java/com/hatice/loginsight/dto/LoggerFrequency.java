package com.hatice.loginsight.dto;

public class LoggerFrequency {

    private String loggerName;
    private long count;

    public LoggerFrequency() {
    }

    public LoggerFrequency(String loggerName, long count) {
        this.loggerName = loggerName;
        this.count = count;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}