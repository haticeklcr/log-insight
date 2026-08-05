package com.hatice.loginsight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "analysis_timeline_stat")
public class AnalysisTimelineStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_analysis_id", nullable = false)
    private Long logAnalysisId;

    @Column(name = "bucket_start", nullable = false)
    private Instant bucketStart;

    @Column(name = "total_count", nullable = false)
    private long totalCount;

    @Column(name = "info_count", nullable = false)
    private long infoCount;

    @Column(name = "warn_count", nullable = false)
    private long warnCount;

    @Column(name = "error_count", nullable = false)
    private long errorCount;

    @Column(name = "exception_count", nullable = false)
    private long exceptionCount;

    public AnalysisTimelineStatEntity() {
    }

    public AnalysisTimelineStatEntity(Long logAnalysisId, Instant bucketStart, long totalCount,
                                       long infoCount, long warnCount, long errorCount, long exceptionCount) {
        this.logAnalysisId = logAnalysisId;
        this.bucketStart = bucketStart;
        this.totalCount = totalCount;
        this.infoCount = infoCount;
        this.warnCount = warnCount;
        this.errorCount = errorCount;
        this.exceptionCount = exceptionCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLogAnalysisId() {
        return logAnalysisId;
    }

    public void setLogAnalysisId(Long logAnalysisId) {
        this.logAnalysisId = logAnalysisId;
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