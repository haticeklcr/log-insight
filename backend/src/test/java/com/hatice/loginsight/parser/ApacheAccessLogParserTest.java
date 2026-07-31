package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApacheAccessLogParserTest {

    private final ApacheAccessLogParser parser = new ApacheAccessLogParser();

    @Test
    void reportsCorrectFormat() {
        assertEquals(LogFormat.APACHE_ACCESS, parser.getFormat());
    }

    @Test
    void canParseAcceptsCombinedFormatLine() {
        String line = "203.0.113.5 - - [01/Jan/2026:12:30:15 +0300] \"GET /index.html HTTP/1.1\" 200 1024 "
                + "\"https://example.com/\" \"Mozilla/5.0\"";
        assertTrue(parser.canParse(line));
    }

    @Test
    void canParseRejectsCommonFormatLine() {
        String line = "192.168.1.10 - - [01/Jan/2026:12:30:15 +0300] \"GET /index.html HTTP/1.1\" 200 1024";
        assertFalse(parser.canParse(line));
    }

    @Test
    void parsesRequestFieldsCorrectly() {
        String line = "203.0.113.7 - - [01/Jan/2026:12:30:17 +0300] \"POST /api/payment HTTP/1.1\" 500 128 "
                + "\"https://example.com/checkout\" \"Mozilla/5.0\"";

        ParsedLogEntry entry = parser.parse(line);

        assertNotNull(entry);
        assertEquals("POST", entry.getMethod());
        assertEquals("/api/payment", entry.getPath());
        assertEquals(500, entry.getStatusCode());
        assertEquals("ERROR", entry.getLevel());
    }
}