package com.hatice.loginsight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_job")
public class AnalysisJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "analysis_name", nullable = false, length = 100)
    private String analysisName;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "progress", nullable = false)
    private int progress;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "analysis_id")
    private Long analysisId;

    @Column(name = "upload_session_id")
    private UUID uploadSessionId;

    @Column(name = "cancel_requested", nullable = false)
    private boolean cancelRequested;

    @Column(name = "resumed_from_checkpoint", nullable = false)
    private boolean resumedFromCheckpoint;

    @Column(name = "requested_parser_type", length = 30)
    private String requestedParserType;

    @Column(name = "detected_log_format", length = 30)
    private String detectedLogFormat;

    @Column(name = "detected_envelope", length = 30)
    private String detectedEnvelope;

    @Column(name = "filter_start_time")
    private Instant filterStartTime;

    @Column(name = "filter_end_time")
    private Instant filterEndTime;

    @Column(name = "filter_levels", length = 200)
    private String filterLevels;

    @Column(name = "filter_logger")
    private String filterLogger;

    @Column(name = "filter_thread")
    private String filterThread;

    @Column(name = "filter_message_contains", length = 500)
    private String filterMessageContains;

    @Column(name = "filter_status_codes", length = 200)
    private String filterStatusCodes;

    @Column(name = "filter_http_methods", length = 200)
    private String filterHttpMethods;

    @Column(name = "filter_path_contains", length = 500)
    private String filterPathContains;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public AnalysisJobEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public UUID getUploadSessionId() {
        return uploadSessionId;
    }

    public void setUploadSessionId(UUID uploadSessionId) {
        this.uploadSessionId = uploadSessionId;
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public void setCancelRequested(boolean cancelRequested) {
        this.cancelRequested = cancelRequested;
    }

    public boolean isResumedFromCheckpoint() {
        return resumedFromCheckpoint;
    }

    public void setResumedFromCheckpoint(boolean resumedFromCheckpoint) {
        this.resumedFromCheckpoint = resumedFromCheckpoint;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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

    public Instant getFilterStartTime() {
        return filterStartTime;
    }

    public void setFilterStartTime(Instant filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    public Instant getFilterEndTime() {
        return filterEndTime;
    }

    public void setFilterEndTime(Instant filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    public String getFilterLevels() {
        return filterLevels;
    }

    public void setFilterLevels(String filterLevels) {
        this.filterLevels = filterLevels;
    }

    public String getFilterLogger() {
        return filterLogger;
    }

    public void setFilterLogger(String filterLogger) {
        this.filterLogger = filterLogger;
    }

    public String getFilterThread() {
        return filterThread;
    }

    public void setFilterThread(String filterThread) {
        this.filterThread = filterThread;
    }

    public String getFilterMessageContains() {
        return filterMessageContains;
    }

    public void setFilterMessageContains(String filterMessageContains) {
        this.filterMessageContains = filterMessageContains;
    }

    public String getFilterStatusCodes() {
        return filterStatusCodes;
    }

    public void setFilterStatusCodes(String filterStatusCodes) {
        this.filterStatusCodes = filterStatusCodes;
    }

    public String getFilterHttpMethods() {
        return filterHttpMethods;
    }

    public void setFilterHttpMethods(String filterHttpMethods) {
        this.filterHttpMethods = filterHttpMethods;
    }

    public String getFilterPathContains() {
        return filterPathContains;
    }

    public void setFilterPathContains(String filterPathContains) {
        this.filterPathContains = filterPathContains;
    }
}