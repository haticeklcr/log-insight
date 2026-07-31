package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogMessageNormalizerTest {

    private final LogMessageNormalizer normalizer = new LogMessageNormalizer();

    @Test
    void normalizesNumericId() {
        assertEquals("User <NUMBER> not found", normalizer.normalize("User 12345 not found"));
    }

    @Test
    void groupsMessagesWithDifferentNumericIdsTheSame() {
        String a = normalizer.normalize("User 98765 not found");
        String b = normalizer.normalize("User 12345 not found");
        assertEquals(a, b);
    }

    @Test
    void normalizesUuid() {
        String message = "Request a1b2c3d4-e5f6-47a8-9b1c-1234567890ab failed";
        assertEquals("Request <UUID> failed", normalizer.normalize(message));
    }

    @Test
    void normalizesIpv4Address() {
        assertEquals("Connection from <IP> refused", normalizer.normalize("Connection from 192.168.1.10 refused"));
    }

    @Test
    void normalizesTimestamp() {
        String message = "Event at 2026-01-01T12:30:15Z recorded";
        assertEquals("Event at <TIMESTAMP> recorded", normalizer.normalize(message));
    }

    @Test
    void normalizesRequestAndTraceIdKeepingKeyName() {
        assertEquals("requestId=<REQUEST_ID>", normalizer.normalize("requestId=abc-123-def"));
        assertEquals("traceId=<TRACE_ID>", normalizer.normalize("traceId=xyz-789"));
    }

    @Test
    void normalizesHexValue() {
        assertEquals("Checksum <HEX> mismatch", normalizer.normalize("Checksum 0x1a2b3c4d mismatch"));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(normalizer.normalize(null));
    }

    @Test
    void leavesPlainTextUnchanged() {
        assertEquals("Application started successfully", normalizer.normalize("Application started successfully"));
    }
}