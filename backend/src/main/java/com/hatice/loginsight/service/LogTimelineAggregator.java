package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisTimelineStatEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LogTimelineAggregator {

    private static final long MINUTE_SECONDS = 60L;
    private static final long HOUR_SECONDS = 3600L;

    private static class BucketCounts {
        int total;
        int info;
        int warn;
        int error;
        int exception;

        void merge(BucketCounts other) {
            total += other.total;
            info += other.info;
            warn += other.warn;
            error += other.error;
            exception += other.exception;
        }
    }

    private final int maxBuckets;
    private long granularitySeconds = MINUTE_SECONDS;
    private Map<Instant, BucketCounts> buckets = new TreeMap<>();

    public LogTimelineAggregator(int maxBuckets) {
        this.maxBuckets = maxBuckets;
    }

    public void record(Instant timestamp, String level, boolean hasException) {
        if (timestamp == null) {
            return;
        }

        Instant bucketStart = floorToGranularity(timestamp, granularitySeconds);

        if (granularitySeconds == MINUTE_SECONDS
                && !buckets.containsKey(bucketStart)
                && buckets.size() >= maxBuckets) {
            rebucketToHourly();
            bucketStart = floorToGranularity(timestamp, granularitySeconds);
        }

        BucketCounts counts = buckets.computeIfAbsent(bucketStart, key -> new BucketCounts());
        counts.total++;
        if ("ERROR".equals(level)) {
            counts.error++;
        } else if ("WARN".equals(level)) {
            counts.warn++;
        } else if ("INFO".equals(level)) {
            counts.info++;
        }
        if (hasException) {
            counts.exception++;
        }
    }

    private void rebucketToHourly() {
        Map<Instant, BucketCounts> rebucketed = new TreeMap<>();
        for (Map.Entry<Instant, BucketCounts> entry : buckets.entrySet()) {
            Instant hourStart = floorToGranularity(entry.getKey(), HOUR_SECONDS);
            rebucketed.computeIfAbsent(hourStart, key -> new BucketCounts()).merge(entry.getValue());
        }
        buckets = rebucketed;
        granularitySeconds = HOUR_SECONDS;
    }

    private Instant floorToGranularity(Instant instant, long granularitySeconds) {
        long epochSecond = instant.getEpochSecond();
        long floored = epochSecond - (epochSecond % granularitySeconds);
        return Instant.ofEpochSecond(floored);
    }

    public List<AnalysisTimelineStatEntity> toEntities(Long logAnalysisId) {
        List<AnalysisTimelineStatEntity> result = new ArrayList<>();
        for (Map.Entry<Instant, BucketCounts> entry : buckets.entrySet()) {
            BucketCounts counts = entry.getValue();
            result.add(new AnalysisTimelineStatEntity(
                    logAnalysisId,
                    entry.getKey(),
                    counts.total,
                    counts.info,
                    counts.warn,
                    counts.error,
                    counts.exception));
        }
        return result;
    }
}