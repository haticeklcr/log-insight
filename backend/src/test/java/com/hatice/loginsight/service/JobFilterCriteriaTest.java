package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.parser.ParsedLogEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobFilterCriteriaTest {

    private ParsedLogEntry entry(String level, String logger, String message, Instant timestamp,
                                  Integer statusCode, String method, String path) {
        ParsedLogEntry entry = new ParsedLogEntry();
        entry.setLevel(level);
        entry.setLogger(logger);
        entry.setMessage(message);
        entry.setTimestamp(timestamp);
        entry.setStatusCode(statusCode);
        entry.setMethod(method);
        entry.setPath(path);
        return entry;
    }

    @Test
    void matchesEverythingWhenNoFiltersSet() {
        JobFilterCriteria criteria = JobFilterCriteria.from(new AnalysisJobEntity());
        assertTrue(criteria.matches(entry("INFO", "L1", "hello", null, null, null, null)));
    }

    @Test
    void filtersOutEntriesOutsideTimeRange() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setFilterStartTime(Instant.parse("2026-01-01T10:00:00Z"));
        job.setFilterEndTime(Instant.parse("2026-01-01T11:00:00Z"));
        JobFilterCriteria criteria = JobFilterCriteria.from(job);

        assertTrue(criteria.matches(entry("INFO", "L1", "a", Instant.parse("2026-01-01T10:30:00Z"), null, null, null)));
        assertFalse(criteria.matches(entry("INFO", "L1", "a", Instant.parse("2026-01-01T09:00:00Z"), null, null, null)));
        assertFalse(criteria.matches(entry("INFO", "L1", "a", Instant.parse("2026-01-01T12:00:00Z"), null, null, null)));
    }

    @Test
    void filtersByLevel() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setFilterLevels("INFO,ERROR");
        JobFilterCriteria criteria = JobFilterCriteria.from(job);

        assertTrue(criteria.matches(entry("ERROR", "L1", "a", null, null, null, null)));
        assertFalse(criteria.matches(entry("WARN", "L1", "a", null, null, null, null)));
    }

    @Test
    void filtersByLoggerContainsCaseInsensitive() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setFilterLogger("payment");
        JobFilterCriteria criteria = JobFilterCriteria.from(job);

        assertTrue(criteria.matches(entry("INFO", "com.example.PaymentService", "a", null, null, null, null)));
        assertFalse(criteria.matches(entry("INFO", "com.example.UserService", "a", null, null, null, null)));
    }

    @Test
    void filtersByStatusCodeAndHttpMethod() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setFilterStatusCodes("404,500");
        job.setFilterHttpMethods("GET");
        JobFilterCriteria criteria = JobFilterCriteria.from(job);

        assertTrue(criteria.matches(entry(null, null, "a", null, 404, "GET", "/x")));
        assertFalse(criteria.matches(entry(null, null, "a", null, 200, "GET", "/x")));
        assertFalse(criteria.matches(entry(null, null, "a", null, 404, "POST", "/x")));
    }

    @Test
    void filtersByMessageContains() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setFilterMessageContains("timeout");
        JobFilterCriteria criteria = JobFilterCriteria.from(job);

        assertTrue(criteria.matches(entry("ERROR", "L1", "Connection timeout occurred", null, null, null, null)));
        assertFalse(criteria.matches(entry("ERROR", "L1", "Connection refused", null, null, null, null)));
    }
}