package com.hatice.loginsight.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UploadSessionStatusResponse {

    private UUID uploadId;
    private String fileName;
    private long fileSize;
    private int chunkSize;
    private long totalChunks;
    private long receivedCount;
    private List<Long> missingChunks;
    private String status;
    private int mergeProgress;
    private Instant expiresAt;

    public UploadSessionStatusResponse() {
    }

    public UploadSessionStatusResponse(UUID uploadId, String fileName, long fileSize, int chunkSize,
                                        long totalChunks, long receivedCount, List<Long> missingChunks,
                                        String status, int mergeProgress, Instant expiresAt) {
        this.uploadId = uploadId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
        this.receivedCount = receivedCount;
        this.missingChunks = missingChunks;
        this.status = status;
        this.mergeProgress = mergeProgress;
        this.expiresAt = expiresAt;
    }

    public UUID getUploadId() {
        return uploadId;
    }

    public void setUploadId(UUID uploadId) {
        this.uploadId = uploadId;
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

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public long getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(long totalChunks) {
        this.totalChunks = totalChunks;
    }

    public long getReceivedCount() {
        return receivedCount;
    }

    public void setReceivedCount(long receivedCount) {
        this.receivedCount = receivedCount;
    }

    public List<Long> getMissingChunks() {
        return missingChunks;
    }

    public void setMissingChunks(List<Long> missingChunks) {
        this.missingChunks = missingChunks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMergeProgress() {
        return mergeProgress;
    }

    public void setMergeProgress(int mergeProgress) {
        this.mergeProgress = mergeProgress;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}