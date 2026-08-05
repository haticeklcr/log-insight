package com.hatice.loginsight.parser;

import java.time.Instant;

public class EnvelopeStripResult {

    private final String payload;
    private final Instant envelopeTimestamp;
    private final String criStream;
    private final String criPartialTag;

    public EnvelopeStripResult(String payload, Instant envelopeTimestamp, String criStream, String criPartialTag) {
        this.payload = payload;
        this.envelopeTimestamp = envelopeTimestamp;
        this.criStream = criStream;
        this.criPartialTag = criPartialTag;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getEnvelopeTimestamp() {
        return envelopeTimestamp;
    }

    public String getCriStream() {
        return criStream;
    }

    public String getCriPartialTag() {
        return criPartialTag;
    }
}