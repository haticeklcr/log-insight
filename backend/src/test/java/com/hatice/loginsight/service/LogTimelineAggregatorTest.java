package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisTimelineStatEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogTimelineAggregatorTest {

    @Test
    void groupsEntriesWithinSameMinuteIntoOneBucket() {
        LogTimelineAggregator aggregator = new LogTimelineAggregator(500);

        aggregator.record(Instant.parse("2026-01-01T10:00:05Z"), "INFO", false);
        aggregator.record(Instant.parse("2026-01-01T10:00:40Z"), "ERROR", true);

        List<AnalysisTimelineStatEntity> entities = aggregator.toEntities(1L);

        assertEquals(1, entities.size());
        assertEquals(2, entities.get(0).getTotalCount());
        assertEquals(1, entities.get(0).getInfoCount());
        assertEquals(1, entities.get(0).getErrorCount());
        assertEquals(1, entities.get(0).getExceptionCount());
    }

    @Test
    void ignoresEntriesWithoutTimestamp() {
        LogTimelineAggregator aggregator = new LogTimelineAggregator(500);

        aggregator.record(null, "INFO", false);

        assertTrue(aggregator.toEntities(1L).isEmpty());
    }

    @Test
    void escalatesToHourlyBucketsWhenLimitExceeded() {
        LogTimelineAggregator aggregator = new LogTimelineAggregator(3);

        Instant base = Instant.parse("2026-01-01T10:00:00Z");
        for (int minute = 0; minute < 5; minute++) {
            aggregator.record(base.plusSeconds(minute * 60L), "INFO", false);
        }

        List<AnalysisTimelineStatEntity> entities = aggregator.toEntities(1L);

        long totalRecorded = entities.stream().mapToLong(AnalysisTimelineStatEntity::getTotalCount).sum();
        assertEquals(5, totalRecorded);
        assertTrue(entities.size() <= 3, "Saatlik birlestirme sonrasi bucket sayisi limiti asmamali");
    }
}