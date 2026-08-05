package com.hatice.loginsight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_thread_stat")
public class AnalysisThreadStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_analysis_id", nullable = false)
    private Long logAnalysisId;

    @Column(name = "thread_name", nullable = false)
    private String threadName;

    @Column(name = "entry_count", nullable = false)
    private long entryCount;

    public AnalysisThreadStatEntity() {
    }

    public AnalysisThreadStatEntity(Long logAnalysisId, String threadName, long entryCount) {
        this.logAnalysisId = logAnalysisId;
        this.threadName = threadName;
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

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(long entryCount) {
        this.entryCount = entryCount;
    }
}