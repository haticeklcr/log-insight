package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NginxAccessLogParserTest {

    private final NginxAccessLogParser parser = new NginxAccessLogParser();

    @Test
    void reportsCorrectFormat() {
        assertEquals(LogFormat.NGINX_ACCESS, parser.getFormat());
    }

    @Test
    void canParseAcceptsCommonFormatLine() {
        String line = "192.168.1.10 - - [01/Jan/2026:12:30:15 +0300] \"GET /index.html HTTP/1.1\" 200 1024";
        assertTrue(parser.canParse(line));
    }

    @Test
    void canParseRejectsCombinedFormatLine() {
        String line = "203.0.113.5 - - [01/Jan/2026:12:30:15 +0300] \"GET /index.html HTTP/1.1\" 200 1024 "
                + "\"https://example.com/\" \"Mozilla/5.0\"";
        assertFalse(parser.canParse(line));
    }

    @Test
    void parsesRequestFieldsAndDerivesLevelFromStatusCode() {
        String line = "192.168.1.12 - - [01/Jan/2026:12:30:17 +0300] \"POST /api/payment HTTP/1.1\" 500 128";

        ParsedLogEntry entry = parser.parse(line);

        assertNotNull(entry);
        assertEquals("POST", entry.getMethod());
        assertEquals("/api/payment", entry.getPath());
        assertEquals(500, entry.getStatusCode());
        assertEquals("ERROR", entry.getLevel());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void derivesWarnLevelForClientErrorStatus() {
        String line = "192.168.1.13 - - [01/Jan/2026:12:30:18 +0300] \"GET /favicon.ico HTTP/1.1\" 404 0";

        ParsedLogEntry entry = parser.parse(line);

        assertNotNull(entry);
        assertEquals("WARN", entry.getLevel());
    }

    @Test
    void derivesInfoLevelForSuccessStatus() {
        String line = "192.168.1.11 - - [01/Jan/2026:12:30:16 +0300] \"GET /api/orders HTTP/1.1\" 200 512";

        ParsedLogEntry entry = parser.parse(line);

        assertNotNull(entry);
        assertEquals("INFO", entry.getLevel());
    }
}