package com.hatice.loginsight.service;

import com.hatice.loginsight.parser.MultilineExceptionInfo;
import com.hatice.loginsight.parser.ParsedLogEntry;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnalysisResultAccumulator {

    private static class ErrorGroup {
        int count;
        String sampleRawMessage;
    }

    private final int maxDistinctLoggers;
    private final int maxDistinctErrorGroups;

    private int totalLines;
    private int parsedEntryCount;
    private int unparsedLineCount;
    private int infoCount;
    private int warningCount;
    private int errorCount;
    private int exceptionCount;
    private int multilineExceptionCount;
    private int timestampPresentCount;
    private int levelPresentCount;
    private int messagePresentCount;
    private Instant firstLogTimestamp;
    private Instant lastLogTimestamp;

    private final Map<String, Integer> loggerCounts = new LinkedHashMap<>();
    private final Map<String, Integer> threadCounts = new LinkedHashMap<>();
    private final Map<Integer, Integer> statusCodeCounts = new LinkedHashMap<>();
    private final Map<String, Integer> httpMethodCounts = new LinkedHashMap<>();
    private final Map<String, ErrorGroup> normalizedErrorGroups = new LinkedHashMap<>();

    public AnalysisResultAccumulator(int maxDistinctLoggers, int maxDistinctErrorGroups) {
        this.maxDistinctLoggers = maxDistinctLoggers;
        this.maxDistinctErrorGroups = maxDistinctErrorGroups;
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
            statusCodeCounts.merge(entry.getStatusCode(), 1, Integer::sum);
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

    private void mergeBounded(Map<String, Integer> counts, String key, int maxDistinct) {
        if (!counts.containsKey(key) && counts.size() >= maxDistinct) {
            return;
        }
        counts.merge(key, 1, Integer::sum);
    }

    public int getTotalLines() {
        return totalLines;
    }

    public int getParsedEntryCount() {
        return parsedEntryCount;
    }

    public int getUnparsedLineCount() {
        return unparsedLineCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getExceptionCount() {
        return exceptionCount;
    }

    public int getMultilineExceptionCount() {
        return multilineExceptionCount;
    }

    public int getTimestampPresentCount() {
        return timestampPresentCount;
    }

    public int getLevelPresentCount() {
        return levelPresentCount;
    }

    public int getMessagePresentCount() {
        return messagePresentCount;
    }

    public Instant getFirstLogTimestamp() {
        return firstLogTimestamp;
    }

    public Instant getLastLogTimestamp() {
        return lastLogTimestamp;
    }

    public Map<String, Integer> getLoggerCounts() {
        return loggerCounts;
    }

    public Map<String, Integer> getThreadCounts() {
        return threadCounts;
    }

    public Map<Integer, Integer> getStatusCodeCounts() {
        return statusCodeCounts;
    }

    public Map<String, Integer> getHttpMethodCounts() {
        return httpMethodCounts;
    }

    public Map<String, String> getNormalizedErrorSampleMessages() {
        Map<String, String> result = new LinkedHashMap<>();
        normalizedErrorGroups.forEach((key, group) -> result.put(key, group.sampleRawMessage));
        return result;
    }

    public Map<String, Integer> getNormalizedErrorCounts() {
        Map<String, Integer> result = new LinkedHashMap<>();
        normalizedErrorGroups.forEach((key, group) -> result.put(key, group.count));
        return result;
    }

    public int computeParseQualityScore() {
        int totalRecords = parsedEntryCount + unparsedLineCount;
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