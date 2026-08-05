package com.hatice.loginsight.parser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MultilineExceptionAggregator {

    private final LogParser activeParser;
    private final int maxStackTraceLines;

    private String currentHeader;
    private Instant currentHeaderEnvelopeTimestamp;
    private boolean currentHeaderIsErrorRecord;
    private List<String> currentContinuation;
    private boolean currentTruncated;
    private boolean inGroup = false;

    public MultilineExceptionAggregator(LogParser activeParser, int maxStackTraceLines) {
        this.activeParser = activeParser;
        this.maxStackTraceLines = maxStackTraceLines;
    }

    public Optional<LogRecordGroup> offer(String rawLine) {
        return offer(rawLine, null);
    }

    public Optional<LogRecordGroup> offer(String rawLine, Instant envelopeTimestamp) {
        boolean continuation = inGroup && isContinuationLine(rawLine);

        if (continuation) {
            appendContinuation(rawLine);
            return Optional.empty();
        }

        Optional<LogRecordGroup> completed = inGroup ? Optional.of(buildGroup()) : Optional.empty();
        startNewGroup(rawLine, envelopeTimestamp);
        return completed;
    }

    public Optional<LogRecordGroup> flush() {
        if (!inGroup) {
            return Optional.empty();
        }
        Optional<LogRecordGroup> completed = Optional.of(buildGroup());
        inGroup = false;
        return completed;
    }

    private void startNewGroup(String headerLine, Instant envelopeTimestamp) {
        currentHeader = headerLine;
        currentHeaderEnvelopeTimestamp = envelopeTimestamp;
        currentHeaderIsErrorRecord = isErrorRecord(headerLine);
        currentContinuation = new ArrayList<>();
        currentTruncated = false;
        inGroup = true;
    }

    private boolean isErrorRecord(String headerLine) {
        ParsedLogEntry entry = activeParser.parse(headerLine);
        return entry != null && "ERROR".equals(entry.getLevel());
    }

    private void appendContinuation(String rawLine) {
        if (currentContinuation.size() < maxStackTraceLines) {
            currentContinuation.add(rawLine);
        } else {
            currentTruncated = true;
        }
    }

    private LogRecordGroup buildGroup() {
        return new LogRecordGroup(currentHeader, currentContinuation, currentTruncated, currentHeaderEnvelopeTimestamp);
    }

    private boolean isContinuationLine(String rawLine) {
        return ContinuationLineDetector.isContinuationLine(rawLine, currentHeaderIsErrorRecord);
    }
}