package com.hatice.loginsight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_http_method_stat")
public class AnalysisHttpMethodStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_analysis_id", nullable = false)
    private Long logAnalysisId;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "entry_count", nullable = false)
    private long entryCount;

    public AnalysisHttpMethodStatEntity() {
    }

    public AnalysisHttpMethodStatEntity(Long logAnalysisId, String httpMethod, long entryCount) {
        this.logAnalysisId = logAnalysisId;
        this.httpMethod = httpMethod;
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

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public long getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(long entryCount) {
        this.entryCount = entryCount;
    }
}