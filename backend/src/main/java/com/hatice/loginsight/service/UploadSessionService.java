package com.hatice.loginsight.service;

import com.hatice.loginsight.dto.UploadSessionStatusResponse;
import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.entity.UploadSessionStatus;
import com.hatice.loginsight.exception.InsufficientDiskSpaceException;
import com.hatice.loginsight.exception.InvalidChunkIndexException;
import com.hatice.loginsight.exception.InvalidChunkSizeException;
import com.hatice.loginsight.exception.InvalidUploadRequestException;
import com.hatice.loginsight.exception.TooManyActiveUploadsException;
import com.hatice.loginsight.exception.UnsupportedFileTypeException;
import com.hatice.loginsight.exception.UploadAlreadyConsumedException;
import com.hatice.loginsight.exception.UploadIncompleteException;
import com.hatice.loginsight.exception.UploadSessionExpiredException;
import com.hatice.loginsight.exception.UploadSessionNotFoundException;
import com.hatice.loginsight.exception.UploadSessionNotInProgressException;
import com.hatice.loginsight.repository.UploadSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UploadSessionService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".log", ".txt");

    private final UploadSessionRepository uploadSessionRepository;
    private final ChunkedUploadStorageService storageService;
    private final UploadMergeRunner uploadMergeRunner;
    private final int chunkSize;
    private final Duration sessionTtl;
    private final int parallelism;
    private final DataSize maxFileSize;
    private final int maxActiveSessions;
    private final DataSize diskReserve;
    private final Object sessionCreationLock = new Object();

    public UploadSessionService(UploadSessionRepository uploadSessionRepository,
                                 ChunkedUploadStorageService storageService,
                                 UploadMergeRunner uploadMergeRunner,
                                 @Value("${app.upload.chunk-size}") DataSize chunkSize,
                                 @Value("${app.upload.session-ttl}") Duration sessionTtl,
                                 @Value("${app.upload.parallelism}") int parallelism,
                                 @Value("${app.upload.max-file-size}") String maxFileSizeRaw,
                                 @Value("${app.upload.max-active-sessions}") int maxActiveSessions,
                                 @Value("${app.upload.disk-reserve}") DataSize diskReserve) {
        this.uploadSessionRepository = uploadSessionRepository;
        this.storageService = storageService;
        this.uploadMergeRunner = uploadMergeRunner;
        this.chunkSize = (int) chunkSize.toBytes();
        this.sessionTtl = sessionTtl;
        this.parallelism = parallelism;
        this.maxFileSize = isBlankOrZero(maxFileSizeRaw) ? null : DataSize.parse(maxFileSizeRaw);
        this.maxActiveSessions = maxActiveSessions;
        this.diskReserve = diskReserve;
    }

    private boolean isBlankOrZero(String value) {
        return value == null || value.isBlank() || value.trim().equals("0");
    }

    public int getParallelism() {
        return parallelism;
    }

    public UploadSessionEntity createSession(String fileName, long fileSize) {
        if (fileName == null || ALLOWED_EXTENSIONS.stream().noneMatch(ext -> fileName.toLowerCase().endsWith(ext))) {
            throw new UnsupportedFileTypeException("Sadece .log ve .txt uzantılı dosyalar desteklenir");
        }
        if (fileSize <= 0) {
            throw new InvalidUploadRequestException("fileSize sıfırdan büyük olmalıdır");
        }
        if (maxFileSize != null && fileSize > maxFileSize.toBytes()) {
            throw new InvalidUploadRequestException(
                    "Dosya boyutu izin verilen maksimum sınırı (" + maxFileSize.toMegabytes() + "MB) aşıyor");
        }

        synchronized (sessionCreationLock) {
            checkActiveSessionLimitAndDiskSpace(fileSize);

            UploadSessionEntity session = new UploadSessionEntity();
            session.setFileName(fileName);
            session.setFileSize(fileSize);
            session.setChunkSize(chunkSize);
            session.setTotalChunks((fileSize + chunkSize - 1) / chunkSize);
            session.setStatus(UploadSessionStatus.IN_PROGRESS);
            session.setMergeProgress(0);
            session.setCreatedAt(Instant.now());
            session.setExpiresAt(Instant.now().plus(sessionTtl));

            UploadSessionEntity saved = uploadSessionRepository.save(session);
            storageService.createSessionDirectory(saved.getId());
            return saved;
        }
    }

    private void checkActiveSessionLimitAndDiskSpace(long newFileSize) {
        List<UploadSessionEntity> activeSessions = uploadSessionRepository.findByStatusIn(
                List.of(UploadSessionStatus.IN_PROGRESS, UploadSessionStatus.MERGING));

        if (activeSessions.size() >= maxActiveSessions) {
            throw new TooManyActiveUploadsException(
                    "Eşzamanlı aktif yükleme oturumu sınırına ulaşıldı: " + maxActiveSessions);
        }

        long totalReserved = activeSessions.stream().mapToLong(this::computeReservedBytes).sum();
        long required = totalReserved + (2 * newFileSize) + diskReserve.toBytes();
        long usableSpace = storageService.getUsableSpace();

        if (usableSpace < required) {
            throw new InsufficientDiskSpaceException(
                    "Yetersiz disk alanı: gerekli " + required + " byte, kullanılabilir " + usableSpace + " byte");
        }
    }

    private long computeReservedBytes(UploadSessionEntity session) {
        return switch (session.getStatus()) {
            case IN_PROGRESS -> (2 * session.getFileSize()) - storageService.computeUploadedBytes(session.getId());
            case MERGING -> session.getFileSize() - storageService.computeMergedBytes(session.getId());
            default -> 0L;
        };
    }

    @Transactional
    public void uploadChunk(UUID uploadId, long chunkIndex, byte[] content) {
        UploadSessionEntity session = getActiveSession(uploadId);

        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new InvalidChunkIndexException(
                    "chunkIndex 0 ile " + (session.getTotalChunks() - 1) + " arasında olmalıdır: " + chunkIndex);
        }

        long expectedSize = expectedChunkSize(session, chunkIndex);
        if (content.length != expectedSize) {
            throw new InvalidChunkSizeException(
                    "Beklenen parça boyutu " + expectedSize + ", gelen: " + content.length);
        }

        storageService.writePart(uploadId, chunkIndex, content);
    }

    private long expectedChunkSize(UploadSessionEntity session, long chunkIndex) {
        boolean isLastChunk = chunkIndex == session.getTotalChunks() - 1;
        if (!isLastChunk) {
            return session.getChunkSize();
        }
        long precedingBytes = (long) session.getChunkSize() * (session.getTotalChunks() - 1);
        return session.getFileSize() - precedingBytes;
    }

    public UploadSessionStatusResponse getStatus(UUID uploadId) {
        UploadSessionEntity session = findSessionOrThrow(uploadId);
        List<Long> missing = storageService.findMissingPartIndices(uploadId, session.getTotalChunks());
        long receivedCount = session.getTotalChunks() - missing.size();

        return new UploadSessionStatusResponse(
                session.getId(), session.getFileName(), session.getFileSize(), session.getChunkSize(),
                session.getTotalChunks(), receivedCount, missing, session.getStatus().name(),
                session.getMergeProgress(), session.getExpiresAt());
    }

    public UploadSessionEntity completeSession(UUID uploadId) {
        UploadSessionEntity session = getActiveSession(uploadId);
        List<Long> missing = storageService.findMissingPartIndices(uploadId, session.getTotalChunks());
        if (!missing.isEmpty()) {
            throw new UploadIncompleteException("Eksik parçalar: " + missing);
        }

        session.setStatus(UploadSessionStatus.MERGING);
        UploadSessionEntity saved;
        try {
            saved = uploadSessionRepository.saveAndFlush(session);
        } catch (OptimisticLockingFailureException e) {
            throw new UploadSessionNotInProgressException(
                    "Oturum eşzamanlı bir istek tarafından zaten tamamlanmaya başlandı: " + uploadId);
        }
        uploadMergeRunner.runMerge(uploadId);
        return saved;
    }

    @Transactional
    public UploadSessionEntity consumeCompletedSession(UUID uploadId) {
        UploadSessionEntity session = findSessionOrThrow(uploadId);
        if (session.getStatus() != UploadSessionStatus.COMPLETED) {
            throw new UploadAlreadyConsumedException(
                    "Oturum COMPLETED durumunda değil (muhtemelen zaten tüketilmiş): " + session.getStatus());
        }
        session.setStatus(UploadSessionStatus.CONSUMED);
        try {
            return uploadSessionRepository.saveAndFlush(session);
        } catch (OptimisticLockingFailureException e) {
            throw new UploadAlreadyConsumedException(
                    "Oturum eşzamanlı bir istek tarafından zaten tüketildi: " + uploadId);
        }
    }

    @Transactional
    public void cancelSession(UUID uploadId) {
        UploadSessionEntity session = findSessionOrThrow(uploadId);
        uploadSessionRepository.delete(session);
        storageService.deleteSessionDirectory(uploadId);
    }

    private UploadSessionEntity findSessionOrThrow(UUID uploadId) {
        return uploadSessionRepository.findById(uploadId)
                .orElseThrow(() -> new UploadSessionNotFoundException("Yükleme oturumu bulunamadı: " + uploadId));
    }

    private UploadSessionEntity getActiveSession(UUID uploadId) {
        UploadSessionEntity session = findSessionOrThrow(uploadId);
        if (session.getStatus() != UploadSessionStatus.IN_PROGRESS) {
            throw new UploadSessionNotInProgressException(
                    "Oturum IN_PROGRESS durumunda değil: " + session.getStatus());
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new UploadSessionExpiredException("Yükleme oturumunun süresi doldu: " + uploadId);
        }
        return session;
    }
}