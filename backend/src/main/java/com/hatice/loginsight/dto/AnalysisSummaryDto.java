package com.hatice.loginsight.dto;

import java.time.Instant;

public class AnalysisSummaryDto {

    private Long id;
    private String fileName;
    private long fileSize;
    private Instant analyzedAt;
    private int totalLines;
    private int errorCount;
    private int exceptionCount;
    private long processingDurationMs;

    public AnalysisSummaryDto() {
    }

    public AnalysisSummaryDto(Long id, String fileName, long fileSize, Instant analyzedAt, int totalLines,
                               int errorCount, int exceptionCount, long processingDurationMs) {
        this.id = id;
        this.fileName = fileName;
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

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
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

    public long getProcessingDurationMs() {
        return processingDurationMs;
    }

    public void setProcessingDurationMs(long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
    }
}