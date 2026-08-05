package com.hatice.loginsight.dto;

import java.util.List;

public class LogAnalysisResult {

    private Long id;
    private String fileName;
    private long totalLines;
    private long infoCount;
    private long warningCount;
    private long errorCount;
    private long exceptionCount;
    private List<ErrorFrequency> mostFrequentErrors;

    public LogAnalysisResult() {
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

    public long getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(long totalLines) {
        this.totalLines = totalLines;
    }

    public long getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(long infoCount) {
        this.infoCount = infoCount;
    }

    public long getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(long warningCount) {
        this.warningCount = warningCount;
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

    public List<ErrorFrequency> getMostFrequentErrors() {
        return mostFrequentErrors;
    }

    public void setMostFrequentErrors(List<ErrorFrequency> mostFrequentErrors) {
        this.mostFrequentErrors = mostFrequentErrors;
    }
}