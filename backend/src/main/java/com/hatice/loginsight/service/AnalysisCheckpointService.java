package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisJobCheckpointEntity;
import com.hatice.loginsight.repository.AnalysisJobCheckpointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnalysisCheckpointService {

    public record CheckpointData(long byteOffset, AnalysisCheckpointSnapshot snapshot) {
    }

    private final AnalysisJobCheckpointRepository checkpointRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisCheckpointService(AnalysisJobCheckpointRepository checkpointRepository) {
        this.checkpointRepository = checkpointRepository;
    }

    public AnalysisCheckpointSnapshot toSnapshot(AnalysisResultAccumulator accumulator) {
        AnalysisCheckpointSnapshot snapshot = new AnalysisCheckpointSnapshot();
        snapshot.setTotalLines(accumulator.getTotalLines());
        snapshot.setParsedEntryCount(accumulator.getParsedEntryCount());
        snapshot.setUnparsedLineCount(accumulator.getUnparsedLineCount());
        snapshot.setInfoCount(accumulator.getInfoCount());
        snapshot.setWarningCount(accumulator.getWarningCount());
        snapshot.setErrorCount(accumulator.getErrorCount());
        snapshot.setExceptionCount(accumulator.getExceptionCount());
        snapshot.setMultilineExceptionCount(accumulator.getMultilineExceptionCount());
        snapshot.setTimestampPresentCount(accumulator.getTimestampPresentCount());
        snapshot.setLevelPresentCount(accumulator.getLevelPresentCount());
        snapshot.setMessagePresentCount(accumulator.getMessagePresentCount());
        snapshot.setFirstLogTimestamp(accumulator.getFirstLogTimestamp());
        snapshot.setLastLogTimestamp(accumulator.getLastLogTimestamp());
        snapshot.setLoggerCounts(accumulator.getLoggerCounts());
        snapshot.setThreadCounts(accumulator.getThreadCounts());
        snapshot.setStatusCodeCounts(accumulator.getStatusCodeCounts());
        snapshot.setHttpMethodCounts(accumulator.getHttpMethodCounts());
        snapshot.setNormalizedErrorSampleMessages(accumulator.getNormalizedErrorSampleMessages());
        snapshot.setNormalizedErrorCounts(accumulator.getNormalizedErrorCounts());
        return snapshot;
    }

    @Transactional
    public void saveCheckpoint(UUID jobId, long byteOffset, AnalysisResultAccumulator accumulator) {
        AnalysisCheckpointSnapshot snapshot = toSnapshot(accumulator);
        String json = objectMapper.writeValueAsString(snapshot);

        AnalysisJobCheckpointEntity entity = checkpointRepository.findById(jobId)
                .orElseGet(AnalysisJobCheckpointEntity::new);
        entity.setJobId(jobId);
        entity.setByteOffset(byteOffset);
        entity.setSnapshotVersion(AnalysisCheckpointSnapshot.CURRENT_SNAPSHOT_VERSION);
        entity.setAccumulatorSnapshot(json);
        entity.setUpdatedAt(Instant.now());
        checkpointRepository.save(entity);
    }

    public Optional<CheckpointData> loadCheckpoint(UUID jobId) {
        return checkpointRepository.findById(jobId)
                .filter(entity -> entity.getSnapshotVersion() == AnalysisCheckpointSnapshot.CURRENT_SNAPSHOT_VERSION)
                .map(entity -> new CheckpointData(
                        entity.getByteOffset(),
                        objectMapper.readValue(entity.getAccumulatorSnapshot(), AnalysisCheckpointSnapshot.class)));
    }

    @Transactional
    public void deleteCheckpoint(UUID jobId) {
        checkpointRepository.findById(jobId).ifPresent(checkpointRepository::delete);
    }
}