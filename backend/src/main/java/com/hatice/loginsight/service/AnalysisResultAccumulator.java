package com.hatice.loginsight.service;

import com.hatice.loginsight.parser.MultilineExceptionInfo;
import com.hatice.loginsight.parser.ParsedLogEntry;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnalysisResultAccumulator {

    private static class ErrorGroup {
        long count;
        String sampleRawMessage;
    }

    private final int maxDistinctLoggers;
    private final int maxDistinctErrorGroups;

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

    private final Map<String, Long> loggerCounts = new LinkedHashMap<>();
    private final Map<String, Long> threadCounts = new LinkedHashMap<>();
    private final Map<Integer, Long> statusCodeCounts = new LinkedHashMap<>();
    private final Map<String, Long> httpMethodCounts = new LinkedHashMap<>();
    private final Map<String, ErrorGroup> normalizedErrorGroups = new LinkedHashMap<>();

    public AnalysisResultAccumulator(int maxDistinctLoggers, int maxDistinctErrorGroups) {
        this.maxDistinctLoggers = maxDistinctLoggers;
        this.maxDistinctErrorGroups = maxDistinctErrorGroups;
    }

    public AnalysisResultAccumulator(int maxDistinctLoggers, int maxDistinctErrorGroups,
                                      AnalysisCheckpointSnapshot snapshot) {
        this(maxDistinctLoggers, maxDistinctErrorGroups);
        this.totalLines = snapshot.getTotalLines();
        this.parsedEntryCount = snapshot.getParsedEntryCount();
        this.unparsedLineCount = snapshot.getUnparsedLineCount();
        this.infoCount = snapshot.getInfoCount();
        this.warningCount = snapshot.getWarningCount();
        this.errorCount = snapshot.getErrorCount();
        this.exceptionCount = snapshot.getExceptionCount();
        this.multilineExceptionCount = snapshot.getMultilineExceptionCount();
        this.timestampPresentCount = snapshot.getTimestampPresentCount();
        this.levelPresentCount = snapshot.getLevelPresentCount();
        this.messagePresentCount = snapshot.getMessagePresentCount();
        this.firstLogTimestamp = snapshot.getFirstLogTimestamp();
        this.lastLogTimestamp = snapshot.getLastLogTimestamp();
        this.loggerCounts.putAll(snapshot.getLoggerCounts());
        this.threadCounts.putAll(snapshot.getThreadCounts());
        this.statusCodeCounts.putAll(snapshot.getStatusCodeCounts());
        this.httpMethodCounts.putAll(snapshot.getHttpMethodCounts());
        snapshot.getNormalizedErrorCounts().forEach((normalizedMessage, count) -> {
            ErrorGroup group = new ErrorGroup();
            group.count = count;
            group.sampleRawMessage = snapshot.getNormalizedErrorSampleMessages().get(normalizedMessage);
            this.normalizedErrorGroups.put(normalizedMessage, group);
        });
    }

    public void incrementTotalLines() {
        totalLines++;
    }

    public void recordUnparsedLine() {
        unparsedLineCount++;
    }

    public void recordEntry(ParsedLogEntry entry, MultilineExceptionInfo exceptionInfo) {
        parsedEntryCount++;

        if (entry.getTimestamp() != null) {
            timestampPresentCount++;
            if (firstLogTimestamp == null || entry.getTimestamp().isBefore(firstLogTimestamp)) {
                firstLogTimestamp = entry.getTimestamp();
            }
            if (lastLogTimestamp == null || entry.getTimestamp().isAfter(lastLogTimestamp)) {
                lastLogTimestamp = entry.getTimestamp();
            }
        }

        if (entry.getLevel() != null) {
            levelPresentCount++;
            switch (entry.getLevel()) {
                case "ERROR" -> errorCount++;
                case "WARN" -> warningCount++;
                case "INFO" -> infoCount++;
                default -> {
                }
            }
        }

        if (entry.getMessage() != null && !entry.getMessage().isBlank()) {
            messagePresentCount++;
        }

        if (exceptionInfo != null) {
            exceptionCount++;
            if (exceptionInfo.isMultiline()) {
                multilineExceptionCount++;
            }
        }

        if (entry.getLogger() != null) {
            mergeBounded(loggerCounts, entry.getLogger(), maxDistinctLoggers);
        }
        if (entry.getThread() != null) {
            mergeBounded(threadCounts, entry.getThread(), maxDistinctLoggers);
        }
        if (entry.getStatusCode() != null) {
            statusCodeCounts.merge(entry.getStatusCode(), 1L, Long::sum);
        }
        if (entry.getMethod() != null) {
            mergeBounded(httpMethodCounts, entry.getMethod(), maxDistinctLoggers);
        }

        if ("ERROR".equals(entry.getLevel()) && entry.getNormalizedMessage() != null
                && !entry.getNormalizedMessage().isBlank()) {
            recordErrorGroup(entry.getNormalizedMessage(), entry.getMessage());
        }
    }

    private void recordErrorGroup(String normalizedMessage, String sampleRawMessage) {
        ErrorGroup group = normalizedErrorGroups.get(normalizedMessage);
        if (group == null) {
            if (normalizedErrorGroups.size() >= maxDistinctErrorGroups) {
                return;
            }
            group = new ErrorGroup();
            group.sampleRawMessage = sampleRawMessage;
            normalizedErrorGroups.put(normalizedMessage, group);
        }
        group.count++;
    }

    private void mergeBounded(Map<String, Long> counts, String key, int maxDistinct) {
        if (!counts.containsKey(key) && counts.size() >= maxDistinct) {
            return;
        }
        counts.merge(key, 1L, Long::sum);
    }

    public long getTotalLines() {
        return totalLines;
    }

    public long getParsedEntryCount() {
        return parsedEntryCount;
    }

    public long getUnparsedLineCount() {
        return unparsedLineCount;
    }

    public long getInfoCount() {
        return infoCount;
    }

    public long getWarningCount() {
        return warningCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public long getExceptionCount() {
        return exceptionCount;
    }

    public long getMultilineExceptionCount() {
        return multilineExceptionCount;
    }

    public long getTimestampPresentCount() {
        return timestampPresentCount;
    }

    public long getLevelPresentCount() {
        return levelPresentCount;
    }

    public long getMessagePresentCount() {
        return messagePresentCount;
    }

    public Instant getFirstLogTimestamp() {
        return firstLogTimestamp;
    }

    public Instant getLastLogTimestamp() {
        return lastLogTimestamp;
    }

    public Map<String, Long> getLoggerCounts() {
        return loggerCounts;
    }

    public Map<String, Long> getThreadCounts() {
        return threadCounts;
    }

    public Map<Integer, Long> getStatusCodeCounts() {
        return statusCodeCounts;
    }

    public Map<String, Long> getHttpMethodCounts() {
        return httpMethodCounts;
    }

    public Map<String, String> getNormalizedErrorSampleMessages() {
        Map<String, String> result = new LinkedHashMap<>();
        normalizedErrorGroups.forEach((key, group) -> result.put(key, group.sampleRawMessage));
        return result;
    }

    public Map<String, Long> getNormalizedErrorCounts() {
        Map<String, Long> result = new LinkedHashMap<>();
        normalizedErrorGroups.forEach((key, group) -> result.put(key, group.count));
        return result;
    }

    public int computeParseQualityScore() {
        long totalRecords = parsedEntryCount + unparsedLineCount;
        if (totalRecords == 0) {
            return 0;
        }
        double parsedRatio = (double) parsedEntryCount / totalRecords;
        double timestampRatio = parsedEntryCount == 0 ? 0 : (double) timestampPresentCount / parsedEntryCount;
        double levelRatio = parsedEntryCount == 0 ? 0 : (double) levelPresentCount / parsedEntryCount;
        double messageRatio = parsedEntryCount == 0 ? 0 : (double) messagePresentCount / parsedEntryCount;

        double average = (parsedRatio + timestampRatio + levelRatio + messageRatio) / 4.0;
        return (int) Math.round(average * 100);
    }
}