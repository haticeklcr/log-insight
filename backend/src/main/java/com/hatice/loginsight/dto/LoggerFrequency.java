package com.hatice.loginsight.dto;

public class LoggerFrequency {

    private String loggerName;
    private int count;

    public LoggerFrequency() {
    }

    public LoggerFrequency(String loggerName, int count) {
        this.loggerName = loggerName;
        this.count = count;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}