package com.hatice.loginsight.dto;

import java.time.Instant;
import java.util.UUID;

public class CreateUploadSessionResponse {

    private UUID uploadId;
    private int chunkSize;
    private long totalChunks;
    private int parallelism;
    private Instant expiresAt;

    public CreateUploadSessionResponse() {
    }

    public CreateUploadSessionResponse(UUID uploadId, int chunkSize, long totalChunks, int parallelism,
                                        Instant expiresAt) {
        this.uploadId = uploadId;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
        this.parallelism = parallelism;
        this.expiresAt = expiresAt;
    }

    public UUID getUploadId() {
        return uploadId;
    }

    public void setUploadId(UUID uploadId) {
        this.uploadId = uploadId;
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

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}