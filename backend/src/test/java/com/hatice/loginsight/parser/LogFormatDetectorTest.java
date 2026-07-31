package com.hatice.loginsight.parser;

import com.hatice.loginsight.exception.LogFormatCouldNotBeDetectedException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogFormatDetectorTest {

    private final LogParserFactory parserFactory = new LogParserFactory(List.of(
            new SpringBootLogParser(), new JsonLogParser(), new NginxAccessLogParser(),
            new ApacheAccessLogParser(), new PlainTextLogParser()));

    private final LogFormatDetector detector = new LogFormatDetector(parserFactory, 50, 60);

    @Test
    void detectsJsonFormatWithFullConfidence() {
        List<String> samples = List.of(
                "{\"level\": \"INFO\", \"message\": \"a\"}",
                "{\"level\": \"WARN\", \"message\": \"b\"}",
                "{\"level\": \"ERROR\", \"message\": \"c\"}");

        LogFormatDetectionResult result = detector.detect(samples);

        assertEquals(LogFormat.JSON, result.getDetectedFormat());
        assertEquals(100, result.getFormatConfidence());
        assertEquals(3, result.getMatchedSampleCount());
    }

    @Test
    void detectsSpringBootFormat() {
        List<String> samples = List.of(
                "2026-01-01 12:30:15.123 INFO 1 --- [main] c.e.Service : hello",
                "2026-01-01 12:30:16.123 ERROR 1 --- [main] c.e.Service : world");

        LogFormatDetectionResult result = detector.detect(samples);

        assertEquals(LogFormat.SPRING_BOOT, result.getDetectedFormat());
        assertEquals(100, result.getFormatConfidence());
    }

    @Test
    void fallsBackToPlainTextBelowConfidenceThreshold() {
        List<String> samples = List.of(
                "{\"level\": \"INFO\", \"message\": \"a\"}",
                "just some unrelated plain text line",
                "another unrelated plain text line",
                "yet another one that is not json at all");

        LogFormatDetectionResult result = detector.detect(samples);

        assertEquals(LogFormat.PLAIN_TEXT, result.getDetectedFormat());
        assertTrue(result.getFormatConfidence() < 60);
    }

    @Test
    void throwsWhenNoSampleLinesAvailable() {
        assertThrows(LogFormatCouldNotBeDetectedException.class, () -> detector.detect(List.of()));
    }

    @Test
    void collectSampleLinesSkipsBlankLinesAndRespectsLimit() throws IOException {
        Path tempFile = Files.createTempFile("log-format-detector-test", ".log");
        try {
            Files.writeString(tempFile, "line1\n\nline2\n\n\nline3\nline4\nline5\n");

            List<String> samples = detector.collectSampleLines(tempFile, 3);

            assertEquals(List.of("line1", "line2", "line3"), samples);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}