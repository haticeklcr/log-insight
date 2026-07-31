package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringBootLogParserTest {

    private final SpringBootLogParser parser = new SpringBootLogParser();

    @Test
    void reportsCorrectFormat() {
        assertEquals(LogFormat.SPRING_BOOT, parser.getFormat());
    }

    @Test
    void canParseRecognizesValidLine() {
        String line = "2026-01-01 12:30:15.123 INFO 12345 --- [nio-8080-exec-1] c.e.payment.PaymentService : Payment received";
        assertTrue(parser.canParse(line));
    }

    @Test
    void canParseRejectsUnrelatedLine() {
        assertFalse(parser.canParse("this is not a spring boot log line"));
        assertFalse(parser.canParse("192.168.1.1 - - [01/Jan/2026:12:00:00 +0300] \"GET / HTTP/1.1\" 200 100"));
    }

    @Test
    void parsesAllFieldsCorrectly() {
        String line = "2026-01-01 12:30:17.789 ERROR 12345 --- [nio-8080-exec-1] c.e.payment.PaymentService : Payment failed for order 4522";

        ParsedLogEntry entry = parser.parse(line);

        assertNotNull(entry);
        assertEquals(LogFormat.SPRING_BOOT, entry.getSourceFormat());
        assertEquals("ERROR", entry.getLevel());
        assertEquals("nio-8080-exec-1", entry.getThread());
        assertEquals("c.e.payment.PaymentService", entry.getLogger());
        assertEquals("Payment failed for order 4522", entry.getMessage());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void parseReturnsNullForUnrecognizedLine() {
        assertNull(parser.parse("plain text that does not match the format"));
    }

    @Test
    void parsesFixtureFileWithoutErrors() throws IOException {
        List<ParsedLogEntry> entries = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("fixtures/spring-boot-sample.log");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ParsedLogEntry entry = parser.parse(line);
                assertNotNull(entry, "Fixture'daki her satır ayrıştırılabilmeli: " + line);
                entries.add(entry);
            }
        }
        assertEquals(7, entries.size());
        long errorCount = entries.stream().filter(e -> "ERROR".equals(e.getLevel())).count();
        assertEquals(3, errorCount);
    }
}