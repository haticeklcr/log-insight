package com.hatice.loginsight.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnalysisCheckpointSnapshot {

    public static final int CURRENT_SNAPSHOT_VERSION = 1;

    private long totalLines;
    private long parsedEntryCount;
    private long unparsedLineCount;
    private long infoCount;
    private long warningCount;
    private long errorCount;
    private long exceptionCount;
    private long multilineExceptionCount;
    private long timestampPresentCount;
    private long levelPresentCount;
    private long messagePresentCount;
    private Instant firstLogTimestamp;
    private Instant lastLogTimestamp;
    private Map<String, Long> loggerCounts = new LinkedHashMap<>();
    private Map<String, Long> threadCounts = new LinkedHashMap<>();
    private Map<Integer, Long> statusCodeCounts = new LinkedHashMap<>();
    private Map<String, Long> httpMethodCounts = new LinkedHashMap<>();
    private Map<String, String> normalizedErrorSampleMessages = new LinkedHashMap<>();
    private Map<String, Long> normalizedErrorCounts = new LinkedHashMap<>();

    public AnalysisCheckpointSnapshot() {
    }

    public long getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(long totalLines) {
        this.totalLines = totalLines;
    }

    public long getParsedEntryCount() {
        return parsedEntryCount;
    }

    public void setParsedEntryCount(long parsedEntryCount) {
        this.parsedEntryCount = parsedEntryCount;
    }

    public long getUnparsedLineCount() {
        return unparsedLineCount;
    }

    public void setUnparsedLineCount(long unparsedLineCount) {
        this.unparsedLineCount = unparsedLineCount;
    }

    public long getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(long infoCount) {
        this.infoCount = infoCount;
    }

    public long getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(long warningCount) {
        this.warningCount = warningCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(long exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    public long getMultilineExceptionCount() {
        return multilineExceptionCount;
    }

    public void setMultilineExceptionCount(long multilineExceptionCount) {
        this.multilineExceptionCount = multilineExceptionCount;
    }

    public long getTimestampPresentCount() {
        return timestampPresentCount;
    }

    public void setTimestampPresentCount(long timestampPresentCount) {
        this.timestampPresentCount = timestampPresentCount;
    }

    public long getLevelPresentCount() {
        return levelPresentCount;
    }

    public void setLevelPresentCount(long levelPresentCount) {
        this.levelPresentCount = levelPresentCount;
    }

    public long getMessagePresentCount() {
        return messagePresentCount;
    }

    public void setMessagePresentCount(long messagePresentCount) {
        this.messagePresentCount = messagePresentCount;
    }

    public Instant getFirstLogTimestamp() {
        return firstLogTimestamp;
    }

    public void setFirstLogTimestamp(Instant firstLogTimestamp) {
        this.firstLogTimestamp = firstLogTimestamp;
    }

    public Instant getLastLogTimestamp() {
        return lastLogTimestamp;
    }

    public void setLastLogTimestamp(Instant lastLogTimestamp) {
        this.lastLogTimestamp = lastLogTimestamp;
    }

    public Map<String, Long> getLoggerCounts() {
        return loggerCounts;
    }

    public void setLoggerCounts(Map<String, Long> loggerCounts) {
        this.loggerCounts = loggerCounts;
    }

    public Map<String, Long> getThreadCounts() {
        return threadCounts;
    }

    public void setThreadCounts(Map<String, Long> threadCounts) {
        this.threadCounts = threadCounts;
    }

    public Map<Integer, Long> getStatusCodeCounts() {
        return statusCodeCounts;
    }

    public void setStatusCodeCounts(Map<Integer, Long> statusCodeCounts) {
        this.statusCodeCounts = statusCodeCounts;
    }

    public Map<String, Long> getHttpMethodCounts() {
        return httpMethodCounts;
    }

    public void setHttpMethodCounts(Map<String, Long> httpMethodCounts) {
        this.httpMethodCounts = httpMethodCounts;
    }

    public Map<String, String> getNormalizedErrorSampleMessages() {
        return normalizedErrorSampleMessages;
    }

    public void setNormalizedErrorSampleMessages(Map<String, String> normalizedErrorSampleMessages) {
        this.normalizedErrorSampleMessages = normalizedErrorSampleMessages;
    }

    public Map<String, Long> getNormalizedErrorCounts() {
        return normalizedErrorCounts;
    }

    public void setNormalizedErrorCounts(Map<String, Long> normalizedErrorCounts) {
        this.normalizedErrorCounts = normalizedErrorCounts;
    }
}