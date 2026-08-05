package com.hatice.loginsight.parser;

import java.time.Instant;

public class AssembledCriRecord {

    private final String payload;
    private final Instant timestamp;
    private final boolean truncated;
    private final boolean incomplete;

    public AssembledCriRecord(String payload, Instant timestamp, boolean truncated, boolean incomplete) {
        this.payload = payload;
        this.timestamp = timestamp;
        this.truncated = truncated;
        this.incomplete = incomplete;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public boolean isIncomplete() {
        return incomplete;
    }
}