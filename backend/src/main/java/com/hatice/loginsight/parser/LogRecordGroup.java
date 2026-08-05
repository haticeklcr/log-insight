package com.hatice.loginsight.parser;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class LogRecordGroup {

    private final String headerLine;
    private final List<String> continuationLines;
    private final boolean truncated;
    private final Instant envelopeTimestamp;

    public LogRecordGroup(String headerLine, List<String> continuationLines, boolean truncated) {
        this(headerLine, continuationLines, truncated, null);
    }

    public LogRecordGroup(String headerLine, List<String> continuationLines, boolean truncated,
                           Instant envelopeTimestamp) {
        this.headerLine = headerLine;
        this.continuationLines = Collections.unmodifiableList(continuationLines);
        this.truncated = truncated;
        this.envelopeTimestamp = envelopeTimestamp;
    }

    public String getHeaderLine() {
        return headerLine;
    }

    public List<String> getContinuationLines() {
        return continuationLines;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public Instant getEnvelopeTimestamp() {
        return envelopeTimestamp;
    }
}