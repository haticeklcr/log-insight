package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonLogParserTest {

    private final JsonLogParser parser = new JsonLogParser();

    @Test
    void reportsCorrectFormat() {
        assertEquals(LogFormat.JSON, parser.getFormat());
    }

    @Test
    void canParseAcceptsValidJsonObject() {
        assertTrue(parser.canParse("{\"level\": \"INFO\", \"message\": \"hello\"}"));
    }

    @Test
    void canParseRejectsNonJsonLine() {
        assertFalse(parser.canParse("2026-01-01 12:30:15 INFO plain text log line"));
    }

    @Test
    void parsesStandardFieldNames() {
        String line = "{\"timestamp\": \"2026-01-01T12:30:15Z\", \"level\": \"INFO\", "
                + "\"logger\": \"com.example.Service\", \"message\": \"hello\", \"thread\": \"main\"}";

        ParsedLogEntry entry = parser.parse(line);

        assertNotNull(entry);
        assertEquals("INFO", entry.getLevel());
        assertEquals("com.example.Service", entry.getLogger());
        assertEquals("hello", entry.getMessage());
        assertEquals("main", entry.getThread());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void parsesAlternativeFieldNames() {
        String line = "{\"time\": \"2026-01-01T12:30:16Z\", \"logLevel\": \"WARN\", "
                + "\"class\": \"com.example.Service\", \"msg\": \"slow\", \"threadName\": \"worker-1\"}";

        ParsedLogEntry entry = parser.parse(line);

        assertNotNull(entry);
        assertEquals("WARN", entry.getLevel());
        assertEquals("com.example.Service", entry.getLogger());
        assertEquals("slow", entry.getMessage());
        assertEquals("worker-1", entry.getThread());
    }

    @Test
    void parsesEpochMillisTimestamp() {
        String line = "{\"timestamp\": 1767270620000, \"level\": \"INFO\", \"message\": \"cache refreshed\"}";

        ParsedLogEntry entry = parser.parse(line);

        assertNotNull(entry);
        assertNotNull(entry.getTimestamp());
        assertEquals(1767270620000L, entry.getTimestamp().toEpochMilli());
    }

    @Test
    void parseReturnsNullForInvalidJson() {
        assertNull(parser.parse("{not valid json"));
    }
}