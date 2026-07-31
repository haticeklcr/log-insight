package com.hatice.loginsight.service;

import com.hatice.loginsight.parser.LogFormat;
import com.hatice.loginsight.parser.MultilineExceptionInfo;
import com.hatice.loginsight.parser.ParsedLogEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisResultAccumulatorTest {

    private ParsedLogEntry entry(String level, String message, String normalizedMessage, String logger,
                                  Instant timestamp) {
        ParsedLogEntry entry = new ParsedLogEntry();
        entry.setSourceFormat(LogFormat.SPRING_BOOT);
        entry.setLevel(level);
        entry.setMessage(message);
        entry.setNormalizedMessage(normalizedMessage);
        entry.setLogger(logger);
        entry.setTimestamp(timestamp);
        return entry;
    }

    @Test
    void countsLevelsCorrectly() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        accumulator.recordEntry(entry("INFO", "a", "a", "L1", Instant.parse("2026-01-01T10:00:00Z")), null);
        accumulator.recordEntry(entry("WARN", "b", "b", "L1", Instant.parse("2026-01-01T10:01:00Z")), null);
        accumulator.recordEntry(entry("ERROR", "c", "c", "L1", Instant.parse("2026-01-01T10:02:00Z")), null);
        accumulator.recordEntry(entry("ERROR", "d", "d", "L1", Instant.parse("2026-01-01T10:03:00Z")), null);

        assertEquals(1, accumulator.getInfoCount());
        assertEquals(1, accumulator.getWarningCount());
        assertEquals(2, accumulator.getErrorCount());
        assertEquals(4, accumulator.getParsedEntryCount());
    }

    @Test
    void tracksFirstAndLastTimestamp() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        accumulator.recordEntry(entry("INFO", "a", "a", "L1", Instant.parse("2026-01-01T10:05:00Z")), null);
        accumulator.recordEntry(entry("INFO", "b", "b", "L1", Instant.parse("2026-01-01T10:00:00Z")), null);
        accumulator.recordEntry(entry("INFO", "c", "c", "L1", Instant.parse("2026-01-01T10:10:00Z")), null);

        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), accumulator.getFirstLogTimestamp());
        assertEquals(Instant.parse("2026-01-01T10:10:00Z"), accumulator.getLastLogTimestamp());
    }

    @Test
    void groupsErrorsByNormalizedMessageKeepingRawSample() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        accumulator.recordEntry(entry("ERROR", "User 98765 not found", "User <NUMBER> not found", "L1", null), null);
        accumulator.recordEntry(entry("ERROR", "User 12345 not found", "User <NUMBER> not found", "L1", null), null);

        assertEquals(1, accumulator.getNormalizedErrorCounts().size());
        assertEquals(2, accumulator.getNormalizedErrorCounts().get("User <NUMBER> not found"));
        assertTrue(accumulator.getNormalizedErrorSampleMessages().get("User <NUMBER> not found").contains("not found"));
    }

    @Test
    void countsUnparsedLinesSeparatelyFromParsedEntries() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        accumulator.recordEntry(entry("INFO", "a", "a", "L1", null), null);
        accumulator.recordUnparsedLine();
        accumulator.recordUnparsedLine();

        assertEquals(1, accumulator.getParsedEntryCount());
        assertEquals(2, accumulator.getUnparsedLineCount());
    }

    @Test
    void countsExceptionsAndMultilineExceptionsSeparately() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        MultilineExceptionInfo singleLine = new MultilineExceptionInfo("NpeException", "msg", "NpeException", "msg", false);
        MultilineExceptionInfo multiline = new MultilineExceptionInfo("NpeException", "msg", "IllegalStateException", "root", true);

        accumulator.recordEntry(entry("ERROR", "a", "a", "L1", null), singleLine);
        accumulator.recordEntry(entry("ERROR", "b", "b", "L1", null), multiline);
        accumulator.recordEntry(entry("INFO", "c", "c", "L1", null), null);

        assertEquals(2, accumulator.getExceptionCount());
        assertEquals(1, accumulator.getMultilineExceptionCount());
    }

    @Test
    void parseQualityScoreIsHighWhenAllFieldsPresent() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        accumulator.recordEntry(entry("INFO", "a", "a", "L1", Instant.now()), null);
        accumulator.recordEntry(entry("INFO", "b", "b", "L1", Instant.now()), null);

        assertEquals(100, accumulator.computeParseQualityScore());
    }

    @Test
    void parseQualityScoreDropsWithUnparsedLines() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        accumulator.recordEntry(entry("INFO", "a", "a", "L1", Instant.now()), null);
        accumulator.recordUnparsedLine();
        accumulator.recordUnparsedLine();
        accumulator.recordUnparsedLine();

        assertTrue(accumulator.computeParseQualityScore() < 100);
    }

    @Test
    void boundsDistinctLoggerCount() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(2, 500);

        accumulator.recordEntry(entry("INFO", "a", "a", "Logger1", null), null);
        accumulator.recordEntry(entry("INFO", "b", "b", "Logger2", null), null);
        accumulator.recordEntry(entry("INFO", "c", "c", "Logger3", null), null);

        assertEquals(2, accumulator.getLoggerCounts().size());
    }

    private ParsedLogEntry httpEntry(String thread, Integer statusCode, String method) {
        ParsedLogEntry entry = new ParsedLogEntry();
        entry.setThread(thread);
        entry.setStatusCode(statusCode);
        entry.setMethod(method);
        entry.setMessage("request");
        entry.setNormalizedMessage("request");
        return entry;
    }

    @Test
    void countsThreadDistributionCorrectly() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        accumulator.recordEntry(httpEntry("worker-1", null, null), null);
        accumulator.recordEntry(httpEntry("worker-1", null, null), null);
        accumulator.recordEntry(httpEntry("worker-2", null, null), null);

        assertEquals(2, accumulator.getThreadCounts().get("worker-1"));
        assertEquals(1, accumulator.getThreadCounts().get("worker-2"));
    }

    @Test
    void countsStatusCodeDistributionCorrectly() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        accumulator.recordEntry(httpEntry(null, 200, null), null);
        accumulator.recordEntry(httpEntry(null, 200, null), null);
        accumulator.recordEntry(httpEntry(null, 404, null), null);

        assertEquals(2, accumulator.getStatusCodeCounts().get(200));
        assertEquals(1, accumulator.getStatusCodeCounts().get(404));
    }

    @Test
    void countsHttpMethodDistributionCorrectly() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);

        accumulator.recordEntry(httpEntry(null, null, "GET"), null);
        accumulator.recordEntry(httpEntry(null, null, "GET"), null);
        accumulator.recordEntry(httpEntry(null, null, "POST"), null);

        assertEquals(2, accumulator.getHttpMethodCounts().get("GET"));
        assertEquals(1, accumulator.getHttpMethodCounts().get("POST"));
    }
}