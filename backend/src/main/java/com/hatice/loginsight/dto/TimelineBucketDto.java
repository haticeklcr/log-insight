package com.hatice.loginsight.dto;

import java.time.Instant;

public class TimelineBucketDto {

    private Instant bucketStart;
    private long totalCount;
    private long infoCount;
    private long warnCount;
    private long errorCount;
    private long exceptionCount;

    public TimelineBucketDto() {
    }

    public TimelineBucketDto(Instant bucketStart, long totalCount, long infoCount, long warnCount,
                              long errorCount, long exceptionCount) {
        this.bucketStart = bucketStart;
        this.totalCount = totalCount;
        this.infoCount = infoCount;
        this.warnCount = warnCount;
        this.errorCount = errorCount;
        this.exceptionCount = exceptionCount;
    }

    public Instant getBucketStart() {
        return bucketStart;
    }

    public void setBucketStart(Instant bucketStart) {
        this.bucketStart = bucketStart;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(long infoCount) {
        this.infoCount = infoCount;
    }

    public long getWarnCount() {
        return warnCount;
    }

    public void setWarnCount(long warnCount) {
        this.warnCount = warnCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(long exceptionCount) {
        this.exceptionCount = exceptionCount;
    }
}