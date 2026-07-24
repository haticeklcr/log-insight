package com.hatice.loginsight.dto;

import com.hatice.loginsight.entity.JobStatus;

import java.time.Instant;
import java.util.UUID;

public class CreateAnalysisJobResponse {

    private UUID jobId;
    private String analysisName;
    private JobStatus status;
    private int progress;
    private Instant createdAt;

    public CreateAnalysisJobResponse() {
    }

    public CreateAnalysisJobResponse(UUID jobId, String analysisName, JobStatus status, int progress, Instant createdAt) {
        this.jobId = jobId;
        this.analysisName = analysisName;
        this.status = status;
        this.progress = progress;
        this.createdAt = createdAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}