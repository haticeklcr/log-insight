package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.parser.ParsedLogEntry;
import com.hatice.loginsight.repository.AnalysisLoggerStatRepository;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AnalysisResultPersisterTest extends AbstractIntegrationTest {

    @Autowired
    private AnalysisResultPersister analysisResultPersister;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    @Autowired
    private AnalysisLoggerStatRepository loggerStatRepository;

    @BeforeEach
    void cleanUp() {
        loggerStatRepository.deleteAllInBatch();
        logAnalysisRepository.deleteAllInBatch();
    }

    private LogAnalysisEntity newEntity() {
        LogAnalysisEntity entity = new LogAnalysisEntity();
        entity.setFileName("test.log");
        entity.setFileSize(10L);
        entity.setTotalLines(1L);
        entity.setInfoCount(1L);
        entity.setWarningCount(0L);
        entity.setErrorCount(0L);
        entity.setExceptionCount(0L);
        entity.setAnalyzedAt(Instant.now());
        entity.setProcessingDurationMs(1L);
        return entity;
    }

    @Test
    void persistsAnalysisAndAllStatsTogether() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);
        ParsedLogEntry entry = new ParsedLogEntry();
        entry.setLevel("INFO");
        entry.setLogger("com.example.Foo");
        accumulator.recordEntry(entry, null);

        LogAnalysisEntity saved = analysisResultPersister.persist(
                newEntity(), accumulator, new LogTimelineAggregator(500));

        assertThat(logAnalysisRepository.findById(saved.getId())).isPresent();
        assertThat(loggerStatRepository.findByLogAnalysisId(saved.getId())).hasSize(1);
    }

    @Test
    void rollsBackWholeResultWhenStatPersistenceFails() {
        AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(200, 500);
        ParsedLogEntry entry = new ParsedLogEntry();
        entry.setLevel("INFO");
        entry.setLogger("x".repeat(500));
        accumulator.recordEntry(entry, null);

        assertThrows(Exception.class, () -> analysisResultPersister.persist(
                newEntity(), accumulator, new LogTimelineAggregator(500)));

        assertThat(logAnalysisRepository.count()).isZero();
    }

    @Test
    void storesCounterLargerThanIntegerMaxValueCorrectly() {
        LogAnalysisEntity entity = newEntity();
        long largeCount = Integer.MAX_VALUE + 1000L;
        entity.setTotalLines(largeCount);

        LogAnalysisEntity saved = analysisResultPersister.persist(
                entity, new AnalysisResultAccumulator(200, 500), new LogTimelineAggregator(500));

        LogAnalysisEntity reloaded = logAnalysisRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTotalLines()).isEqualTo(largeCount);
    }
}