package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlainTextLogParserTest {

    private final PlainTextLogParser parser = new PlainTextLogParser();

    @Test
    void reportsCorrectFormat() {
        assertEquals(LogFormat.PLAIN_TEXT, parser.getFormat());
    }

    @Test
    void canParseAlwaysReturnsTrue() {
        assertTrue(parser.canParse("hicbir yapiya uymayan rastgele bir satir"));
        assertTrue(parser.canParse(""));
    }

    @Test
    void normalizesWarningToWarn() {
        ParsedLogEntry entry = parser.parse("2026-07-21 10:00:25 WARNING Disk usage above threshold: 85%");

        assertNotNull(entry);
        assertEquals("WARN", entry.getLevel());
    }

    @Test
    void recognizesAllStandardLevels() {
        assertEquals("INFO", parser.parse("2026-07-21 10:00:01 INFO Application started").getLevel());
        assertEquals("WARN", parser.parse("2026-07-21 10:00:12 WARN Slow response").getLevel());
        assertEquals("ERROR", parser.parse("2026-07-21 10:00:20 ERROR Connection refused").getLevel());
        assertEquals("DEBUG", parser.parse("2026-07-21 10:00:35 DEBUG Cache lookup miss").getLevel());
        assertEquals("TRACE", parser.parse("2026-07-21 10:00:40 TRACE Entering method").getLevel());
    }

    @Test
    void detectsExceptionType() {
        ParsedLogEntry entry = parser.parse("2026-07-21 10:00:30 ERROR NullPointerException at PaymentService.java:88");

        assertNotNull(entry);
        assertEquals("NullPointerException", entry.getExceptionType());
    }

    @Test
    void fallsBackGracefullyForUnrecognizedLine() {
        ParsedLogEntry entry = parser.parse("this is just a random line with no recognizable level");

        assertNotNull(entry);
        assertEquals("this is just a random line with no recognizable level", entry.getMessage());
    }
}