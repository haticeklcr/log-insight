package com.hatice.loginsight.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "log_analysis")
public class LogAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "total_lines", nullable = false)
    private int totalLines;

    @Column(name = "info_count", nullable = false)
    private int infoCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "exception_count", nullable = false)
    private int exceptionCount;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    @Column(name = "processing_duration_ms", nullable = false)
    private long processingDurationMs;

    @Column(name = "analysis_name")
    private String analysisName;

    @OneToMany(mappedBy = "logAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FrequentErrorEntity> frequentErrors = new ArrayList<>();

    public LogAnalysisEntity() {
    }

    public void addFrequentError(FrequentErrorEntity error) {
        frequentErrors.add(error);
        error.setLogAnalysis(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(int infoCount) {
        this.infoCount = infoCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
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

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Instant analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public long getProcessingDurationMs() {
        return processingDurationMs;
    }

    public void setProcessingDurationMs(long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
    }

    public List<FrequentErrorEntity> getFrequentErrors() {
        return frequentErrors;
    }

    public void setFrequentErrors(List<FrequentErrorEntity> frequentErrors) {
        this.frequentErrors = frequentErrors;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }
}