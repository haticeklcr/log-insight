package com.hatice.loginsight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_logger_stat")
public class AnalysisLoggerStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_analysis_id", nullable = false)
    private Long logAnalysisId;

    @Column(name = "logger_name", nullable = false)
    private String loggerName;

    @Column(name = "entry_count", nullable = false)
    private long entryCount;

    public AnalysisLoggerStatEntity() {
    }

    public AnalysisLoggerStatEntity(Long logAnalysisId, String loggerName, long entryCount) {
        this.logAnalysisId = logAnalysisId;
        this.loggerName = loggerName;
        this.entryCount = entryCount;
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

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public long getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(long entryCount) {
        this.entryCount = entryCount;
    }
}