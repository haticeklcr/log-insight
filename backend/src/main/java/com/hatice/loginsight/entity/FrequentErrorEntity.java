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

    @Column(name = "normalized_message", length = 1000)
    private String normalizedMessage;

    @Column(name = "occurrence_count", nullable = false)
    private long occurrenceCount;

    public FrequentErrorEntity() {
    }

    public FrequentErrorEntity(String message, long occurrenceCount) {
        this.message = message;
        this.occurrenceCount = occurrenceCount;
    }

    public FrequentErrorEntity(String message, String normalizedMessage, long occurrenceCount) {
        this.message = message;
        this.normalizedMessage = normalizedMessage;
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

    public String getNormalizedMessage() {
        return normalizedMessage;
    }

    public void setNormalizedMessage(String normalizedMessage) {
        this.normalizedMessage = normalizedMessage;
    }

    public long getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(long occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }
}