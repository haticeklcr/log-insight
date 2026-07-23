package com.hatice.loginsight.dto;

import java.time.Instant;
import java.util.List;

public class AnalysisDetailDto {

    private Long id;
    private String fileName;
    private long fileSize;
    private Instant analyzedAt;
    private long processingDurationMs;
    private int totalLines;
    private int infoCount;
    private int warningCount;
    private int errorCount;
    private int exceptionCount;
    private List<ErrorFrequency> mostFrequentErrors;

    public AnalysisDetailDto() {
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

    public long getProcessingDurationMs() {
        return processingDurationMs;
    }

    public void setProcessingDurationMs(long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
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

    public List<ErrorFrequency> getMostFrequentErrors() {
        return mostFrequentErrors;
    }

    public void setMostFrequentErrors(List<ErrorFrequency> mostFrequentErrors) {
        this.mostFrequentErrors = mostFrequentErrors;
    }
}