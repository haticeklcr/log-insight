package com.hatice.loginsight.dto;

import com.hatice.loginsight.entity.JobStatus;

import java.time.Instant;
import java.util.UUID;

public class AnalysisJobDetailDto {

    private UUID jobId;
    private String analysisName;
    private String fileName;
    private long fileSize;
    private JobStatus status;
    private int progress;
    private int retryCount;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorCode;
    private Long analysisId;
    private String requestedParserType;
    private String detectedLogFormat;
    private String detectedEnvelope;
    private AppliedFiltersDto appliedFilters;

    public AnalysisJobDetailDto() {
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
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

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public String getRequestedParserType() {
        return requestedParserType;
    }

    public void setRequestedParserType(String requestedParserType) {
        this.requestedParserType = requestedParserType;
    }

    public String getDetectedLogFormat() {
        return detectedLogFormat;
    }

    public void setDetectedLogFormat(String detectedLogFormat) {
        this.detectedLogFormat = detectedLogFormat;
    }

    public String getDetectedEnvelope() {
        return detectedEnvelope;
    }

    public void setDetectedEnvelope(String detectedEnvelope) {
        this.detectedEnvelope = detectedEnvelope;
    }

    public AppliedFiltersDto getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(AppliedFiltersDto appliedFilters) {
        this.appliedFilters = appliedFilters;
    }
}