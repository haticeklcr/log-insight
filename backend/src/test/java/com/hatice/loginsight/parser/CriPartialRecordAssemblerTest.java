package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriPartialRecordAssemblerTest {

    @Test
    void mergesConsecutivePartialsWithFollowingFullIntoOneRecord() {
        CriPartialRecordAssembler assembler = new CriPartialRecordAssembler(1000);
        Instant firstTimestamp = Instant.parse("2026-07-30T06:55:08.100Z");

        List<AssembledCriRecord> emptyResult = assembler.offer("stdout", "P", "Hello ", firstTimestamp);
        assertTrue(emptyResult.isEmpty());

        List<AssembledCriRecord> completed = assembler.offer("stdout", "F", "World",
                Instant.parse("2026-07-30T06:55:08.200Z"));

        assertEquals(1, completed.size());
        AssembledCriRecord record = completed.get(0);
        assertEquals("Hello World", record.getPayload());
        assertEquals(firstTimestamp, record.getTimestamp());
        assertFalse(record.isIncomplete());
        assertFalse(record.isTruncated());
    }

    @Test
    void mergedRecordIsCountedAsSingleLogicalRecord() {
        CriPartialRecordAssembler assembler = new CriPartialRecordAssembler(1000);
        assembler.offer("stdout", "P", "parca-1-", Instant.parse("2026-07-30T06:55:08.100Z"));
        assembler.offer("stdout", "P", "parca-2-", Instant.parse("2026-07-30T06:55:08.150Z"));
        List<AssembledCriRecord> completed = assembler.offer("stdout", "F", "parca-3",
                Instant.parse("2026-07-30T06:55:08.200Z"));

        assertEquals(1, completed.size());
        assertEquals("parca-1-parca-2-parca-3", completed.get(0).getPayload());
    }

    @Test
    void doesNotMixStdoutAndStderrFragments() {
        CriPartialRecordAssembler assembler = new CriPartialRecordAssembler(1000);
        assembler.offer("stdout", "P", "yarim-stdout", Instant.parse("2026-07-30T06:55:08.100Z"));

        List<AssembledCriRecord> results = assembler.offer("stderr", "F", "tam-stderr",
                Instant.parse("2026-07-30T06:55:08.200Z"));

        assertEquals(2, results.size());
        assertTrue(results.get(0).isIncomplete());
        assertEquals("yarim-stdout", results.get(0).getPayload());
        assertFalse(results.get(1).isIncomplete());
        assertEquals("tam-stderr", results.get(1).getPayload());
    }

    @Test
    void terminatesOpenGroupWhenUnexpectedStreamArrives() {
        CriPartialRecordAssembler assembler = new CriPartialRecordAssembler(1000);
        assembler.offer("stdout", "P", "acik-parca", Instant.parse("2026-07-30T06:55:08.100Z"));

        List<AssembledCriRecord> results = assembler.offer("stdout", "P", "baska-stream-oncesi",
                Instant.parse("2026-07-30T06:55:08.150Z"));

        assertTrue(results.isEmpty());
    }

    @Test
    void incompleteRecordAtEndOfFileIsNotSilentlySuccessful() {
        CriPartialRecordAssembler assembler = new CriPartialRecordAssembler(1000);
        assembler.offer("stdout", "P", "yarim-kalan", Instant.parse("2026-07-30T06:55:08.100Z"));

        List<AssembledCriRecord> flushed = assembler.flush();

        assertEquals(1, flushed.size());
        assertTrue(flushed.get(0).isIncomplete());
        assertEquals("yarim-kalan", flushed.get(0).getPayload());
    }

    @Test
    void limitExceedingMergedRecordIsNotSilentlyTruncated() {
        CriPartialRecordAssembler assembler = new CriPartialRecordAssembler(10);
        assembler.offer("stdout", "P", "12345", Instant.parse("2026-07-30T06:55:08.100Z"));

        List<AssembledCriRecord> completed = assembler.offer("stdout", "F", "67890ABCDE",
                Instant.parse("2026-07-30T06:55:08.200Z"));

        assertEquals(1, completed.size());
        AssembledCriRecord record = completed.get(0);
        assertEquals(10, record.getPayload().length());
        assertTrue(record.isTruncated());
    }

    @Test
    void outerTimestampIsTakenFromFirstPartialFragment() {
        CriPartialRecordAssembler assembler = new CriPartialRecordAssembler(1000);
        Instant firstTimestamp = Instant.parse("2026-07-30T06:55:08.100Z");
        assembler.offer("stdout", "P", "a", firstTimestamp);
        assembler.offer("stdout", "P", "b", Instant.parse("2026-07-30T06:55:08.150Z"));

        List<AssembledCriRecord> completed = assembler.offer("stdout", "F", "c",
                Instant.parse("2026-07-30T06:55:08.200Z"));

        assertEquals(firstTimestamp, completed.get(0).getTimestamp());
    }

    @Test
    void assemblyMemoryHoldsOnlyOneOpenRecordAtATime() {
        CriPartialRecordAssembler assembler = new CriPartialRecordAssembler(5);
        assembler.offer("stdout", "P", "abcde", Instant.parse("2026-07-30T06:55:08.100Z"));

        List<AssembledCriRecord> completed = assembler.offer("stdout", "F", "fghij",
                Instant.parse("2026-07-30T06:55:08.200Z"));

        assertEquals(1, completed.size());
        assertEquals(5, completed.get(0).getPayload().length());
        assertTrue(completed.get(0).isTruncated());
    }

    @Test
    void nonCriLineWhileBufferOpenClosesBufferAndPassesLineThrough() {
        CriPartialRecordAssembler assembler = new CriPartialRecordAssembler(1000);
        assembler.offer("stdout", "P", "acik-parca", Instant.parse("2026-07-30T06:55:08.100Z"));

        List<AssembledCriRecord> results = assembler.offer(null, null, "duz-satir", null);

        assertEquals(2, results.size());
        assertTrue(results.get(0).isIncomplete());
        assertFalse(results.get(1).isIncomplete());
        assertEquals("duz-satir", results.get(1).getPayload());
    }
}