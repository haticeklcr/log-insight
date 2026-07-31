package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionInfoExtractorTest {

    private final ExceptionInfoExtractor extractor = new ExceptionInfoExtractor();

    @Test
    void extractsRootCauseFromCausedByChain() {
        LogRecordGroup group = new LogRecordGroup(
                "2026-01-01 12:30:30.000 ERROR 1 --- [main] c.e.Service : failed",
                List.of(
                        "java.lang.NullPointerException: gateway is null",
                        "\tat com.example.A.a(A.java:1)",
                        "Caused by: java.lang.IllegalStateException: connection pool exhausted",
                        "\tat com.example.B.b(B.java:2)",
                        "\t... 3 more"),
                false);

        MultilineExceptionInfo info = extractor.extract(group);

        assertNotNull(info);
        assertEquals("java.lang.NullPointerException", info.getExceptionType());
        assertEquals("java.lang.IllegalStateException", info.getRootCauseType());
        assertEquals("connection pool exhausted", info.getRootCauseMessage());
        assertTrue(info.isMultiline());
    }

    @Test
    void singleLineExceptionHasSameTypeAndRootCause() {
        LogRecordGroup group = new LogRecordGroup(
                "2026-01-01 12:30:30.000 ERROR 1 --- [main] c.e.Service : NullPointerException: gateway is null",
                List.of(),
                false);

        MultilineExceptionInfo info = extractor.extract(group);

        assertNotNull(info);
        assertEquals("NullPointerException", info.getExceptionType());
        assertEquals("NullPointerException", info.getRootCauseType());
        assertEquals("gateway is null", info.getRootCauseMessage());
        assertTrue(info.isMultiline() == false);
    }

    @Test
    void returnsNullWhenNoExceptionPresent() {
        LogRecordGroup group = new LogRecordGroup(
                "2026-01-01 12:30:30.000 INFO 1 --- [main] c.e.Service : everything is fine",
                List.of(),
                false);

        assertNull(extractor.extract(group));
    }
}