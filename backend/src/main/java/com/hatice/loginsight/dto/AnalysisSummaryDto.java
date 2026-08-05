package com.hatice.loginsight.dto;

import java.time.Instant;

public class AnalysisSummaryDto {

    private Long id;
    private String fileName;
    private String analysisName;
    private long fileSize;
    private Instant analyzedAt;
    private long totalLines;
    private long errorCount;
    private long exceptionCount;
    private long processingDurationMs;

    public AnalysisSummaryDto() {
    }

    public AnalysisSummaryDto(Long id, String fileName, String analysisName, long fileSize, Instant analyzedAt,
                               long totalLines, long errorCount, long exceptionCount, long processingDurationMs) {
        this.id = id;
        this.fileName = fileName;
        this.analysisName = analysisName;
        this.fileSize = fileSize;
        this.analyzedAt = analyzedAt;
        this.totalLines = totalLines;
        this.errorCount = errorCount;
        this.exceptionCount = exceptionCount;
        this.processingDurationMs = processingDurationMs;
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

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Instant analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public long getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(long totalLines) {
        this.totalLines = totalLines;
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

    public long getProcessingDurationMs() {
        return processingDurationMs;
    }

    public void setProcessingDurationMs(long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
    }
}