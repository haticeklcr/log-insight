package com.hatice.loginsight.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MultilineExceptionAggregator {

    private static final Pattern MORE_PATTERN = Pattern.compile("^\\.\\.\\.\\s*\\d+\\s*more$");
    private static final Pattern CAUSED_BY_PATTERN = Pattern.compile("^Caused by:.*$");
    private static final Pattern SUPPRESSED_PATTERN = Pattern.compile("^Suppressed:.*$");
    private static final Pattern BARE_EXCEPTION_PATTERN =
            Pattern.compile("^[\\w$]+(\\.[\\w$]+)*Exception:?.*$");

    private final LogParser activeParser;
    private final int maxStackTraceLines;

    private String currentHeader;
    private List<String> currentContinuation;
    private boolean currentTruncated;
    private boolean inGroup = false;

    public MultilineExceptionAggregator(LogParser activeParser, int maxStackTraceLines) {
        this.activeParser = activeParser;
        this.maxStackTraceLines = maxStackTraceLines;
    }

    public Optional<LogRecordGroup> offer(String rawLine) {
        boolean continuation = inGroup && isContinuationLine(rawLine);

        if (continuation) {
            appendContinuation(rawLine);
            return Optional.empty();
        }

        Optional<LogRecordGroup> completed = inGroup ? Optional.of(buildGroup()) : Optional.empty();
        startNewGroup(rawLine);
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

    private void startNewGroup(String headerLine) {
        currentHeader = headerLine;
        currentContinuation = new ArrayList<>();
        currentTruncated = false;
        inGroup = true;
    }

    private void appendContinuation(String rawLine) {
        if (currentContinuation.size() < maxStackTraceLines) {
            currentContinuation.add(rawLine);
        } else {
            currentTruncated = true;
        }
    }

    private LogRecordGroup buildGroup() {
        return new LogRecordGroup(currentHeader, currentContinuation, currentTruncated);
    }

    private boolean isContinuationLine(String rawLine) {
        String trimmed = rawLine.trim();
        if (trimmed.startsWith("at ")
                || MORE_PATTERN.matcher(trimmed).matches()
                || CAUSED_BY_PATTERN.matcher(trimmed).matches()
                || SUPPRESSED_PATTERN.matcher(trimmed).matches()
                || BARE_EXCEPTION_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        return !activeParser.canParse(rawLine);
    }
}