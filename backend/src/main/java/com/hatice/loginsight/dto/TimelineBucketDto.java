package com.hatice.loginsight.dto;

import java.time.Instant;

public class TimelineBucketDto {

    private Instant bucketStart;
    private int totalCount;
    private int infoCount;
    private int warnCount;
    private int errorCount;
    private int exceptionCount;

    public TimelineBucketDto() {
    }

    public TimelineBucketDto(Instant bucketStart, int totalCount, int infoCount, int warnCount,
                              int errorCount, int exceptionCount) {
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

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(int infoCount) {
        this.infoCount = infoCount;
    }

    public int getWarnCount() {
        return warnCount;
    }

    public void setWarnCount(int warnCount) {
        this.warnCount = warnCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(int exceptionCount) {
        this.exceptionCount = exceptionCount;
    }
}