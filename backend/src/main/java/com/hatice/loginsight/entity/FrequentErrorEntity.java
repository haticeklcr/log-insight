package com.hatice.loginsight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "frequent_error")
public class FrequentErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private LogAnalysisEntity logAnalysis;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    public FrequentErrorEntity() {
    }

    public FrequentErrorEntity(String message, int occurrenceCount) {
        this.message = message;
        this.occurrenceCount = occurrenceCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LogAnalysisEntity getLogAnalysis() {
        return logAnalysis;
    }

    public void setLogAnalysis(LogAnalysisEntity logAnalysis) {
        this.logAnalysis = logAnalysis;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }
}