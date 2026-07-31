package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultilineExceptionAggregatorTest {

    private final SpringBootLogParser parser = new SpringBootLogParser();

    @Test
    void groupsStackTraceLinesIntoSingleRecord() {
        MultilineExceptionAggregator aggregator = new MultilineExceptionAggregator(parser, 500);

        String header1 = "2026-01-01 12:30:30.000 ERROR 12345 --- [nio-8080-exec-5] c.e.payment.PaymentService : Payment processing failed";
        String[] continuationLines = {
                "java.lang.NullPointerException: Cannot invoke \"PaymentGateway.charge()\" because gateway is null",
                "\tat com.example.payment.PaymentService.process(PaymentService.java:45)",
                "\tat com.example.payment.PaymentController.handle(PaymentController.java:22)",
                "Caused by: java.lang.IllegalStateException: connection pool exhausted",
                "\tat com.example.db.ConnectionPool.borrow(ConnectionPool.java:88)",
                "\t... 3 more"
        };
        String header2 = "2026-01-01 12:30:31.000 INFO 12345 --- [nio-8080-exec-6] c.e.payment.PaymentService : Retry scheduled";

        assertTrue(aggregator.offer(header1).isEmpty());
        for (String line : continuationLines) {
            assertTrue(aggregator.offer(line).isEmpty(), "Devam satırı yeni grup açmamalı: " + line);
        }

        Optional<LogRecordGroup> completedGroup = aggregator.offer(header2);

        assertTrue(completedGroup.isPresent());
        assertEquals(header1, completedGroup.get().getHeaderLine());
        assertEquals(6, completedGroup.get().getContinuationLines().size());
        assertFalse(completedGroup.get().isTruncated());

        Optional<LogRecordGroup> flushed = aggregator.flush();
        assertTrue(flushed.isPresent());
        assertEquals(header2, flushed.get().getHeaderLine());
        assertEquals(0, flushed.get().getContinuationLines().size());
    }

    @Test
    void truncatesContinuationLinesBeyondLimit() {
        MultilineExceptionAggregator aggregator = new MultilineExceptionAggregator(parser, 2);

        aggregator.offer("2026-01-01 12:30:30.000 ERROR 1 --- [main] c.e.Service : failed");
        aggregator.offer("\tat com.example.A.a(A.java:1)");
        aggregator.offer("\tat com.example.B.b(B.java:2)");
        aggregator.offer("\tat com.example.C.c(C.java:3)");

        Optional<LogRecordGroup> group = aggregator.flush();

        assertTrue(group.isPresent());
        assertEquals(2, group.get().getContinuationLines().size());
        assertTrue(group.get().isTruncated());
    }

    @Test
    void singleLineRecordHasNoContinuationLines() {
        MultilineExceptionAggregator aggregator = new MultilineExceptionAggregator(parser, 500);

        aggregator.offer("2026-01-01 12:30:30.000 INFO 1 --- [main] c.e.Service : just an info line");

        Optional<LogRecordGroup> group = aggregator.flush();

        assertTrue(group.isPresent());
        assertTrue(group.get().getContinuationLines().isEmpty());
    }
}