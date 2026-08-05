package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisTimelineStatEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LogTimelineAggregator {

    private static final long[] GRANULARITY_SECONDS = {
            60L, 300L, 900L, 3600L, 21600L, 86400L, 604800L
    };

    private static class BucketCounts {
        long total;
        long info;
        long warn;
        long error;
        long exception;

        void merge(BucketCounts other) {
            total += other.total;
            info += other.info;
            warn += other.warn;
            error += other.error;
            exception += other.exception;
        }
    }

    private final int maxBuckets;
    private int granularityIndex = 0;
    private Map<Instant, BucketCounts> buckets = new TreeMap<>();

    public LogTimelineAggregator(int maxBuckets) {
        this.maxBuckets = maxBuckets;
    }

    public void record(Instant timestamp, String level, boolean hasException) {
        if (timestamp == null) {
            return;
        }

        Instant bucketStart = floorToGranularity(timestamp, GRANULARITY_SECONDS[granularityIndex]);

        while (!buckets.containsKey(bucketStart)
                && buckets.size() >= maxBuckets
                && granularityIndex < GRANULARITY_SECONDS.length - 1) {
            rebucketToNextGranularity();
            bucketStart = floorToGranularity(timestamp, GRANULARITY_SECONDS[granularityIndex]);
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

    private void rebucketToNextGranularity() {
        granularityIndex++;
        long nextGranularitySeconds = GRANULARITY_SECONDS[granularityIndex];
        Map<Instant, BucketCounts> rebucketed = new TreeMap<>();
        for (Map.Entry<Instant, BucketCounts> entry : buckets.entrySet()) {
            Instant newBucketStart = floorToGranularity(entry.getKey(), nextGranularitySeconds);
            rebucketed.computeIfAbsent(newBucketStart, key -> new BucketCounts()).merge(entry.getValue());
        }
        buckets = rebucketed;
    }

    private Instant floorToGranularity(Instant instant, long granularitySeconds) {
        long epochSecond = instant.getEpochSecond();
        long floored = epochSecond - (epochSecond % granularitySeconds);
        return Instant.ofEpochSecond(floored);
    }

    public String getGranularityName() {
        return switch (granularityIndex) {
            case 0 -> "MINUTE";
            case 1 -> "FIVE_MINUTES";
            case 2 -> "FIFTEEN_MINUTES";
            case 3 -> "HOUR";
            case 4 -> "SIX_HOURS";
            case 5 -> "DAY";
            default -> "WEEK";
        };
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