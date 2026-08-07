package com.hatice.loginsight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_job_checkpoint")
public class AnalysisJobCheckpointEntity {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "byte_offset", nullable = false)
    private long byteOffset;

    @Column(name = "snapshot_version", nullable = false)
    private int snapshotVersion;

    @Column(name = "accumulator_snapshot", nullable = false, columnDefinition = "TEXT")
    private String accumulatorSnapshot;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public long getByteOffset() {
        return byteOffset;
    }

    public void setByteOffset(long byteOffset) {
        this.byteOffset = byteOffset;
    }

    public int getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(int snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public String getAccumulatorSnapshot() {
        return accumulatorSnapshot;
    }

    public void setAccumulatorSnapshot(String accumulatorSnapshot) {
        this.accumulatorSnapshot = accumulatorSnapshot;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}