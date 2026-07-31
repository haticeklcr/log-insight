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
    private int totalCount;

    @Column(name = "info_count", nullable = false)
    private int infoCount;

    @Column(name = "warn_count", nullable = false)
    private int warnCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "exception_count", nullable = false)
    private int exceptionCount;

    public AnalysisTimelineStatEntity() {
    }

    public AnalysisTimelineStatEntity(Long logAnalysisId, Instant bucketStart, int totalCount,
                                       int infoCount, int warnCount, int errorCount, int exceptionCount) {
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