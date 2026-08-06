package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.entity.UploadSessionStatus;
import com.hatice.loginsight.repository.UploadSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UploadMergeRunner {

    private static final Logger log = LoggerFactory.getLogger(UploadMergeRunner.class);

    private final UploadSessionRepository uploadSessionRepository;
    private final ChunkedUploadStorageService storageService;
    private final long mergeProgressIntervalMs;

    public UploadMergeRunner(UploadSessionRepository uploadSessionRepository,
                              ChunkedUploadStorageService storageService,
                              @Value("${app.upload.merge-progress-interval}") long mergeProgressIntervalMs) {
        this.uploadSessionRepository = uploadSessionRepository;
        this.storageService = storageService;
        this.mergeProgressIntervalMs = mergeProgressIntervalMs;
    }

    @Async("uploadMergeTaskExecutor")
    public void runMerge(UUID uploadId) {
        Optional<UploadSessionEntity> maybeSession = uploadSessionRepository.findById(uploadId);
        if (maybeSession.isEmpty()) {
            return;
        }
        UploadSessionEntity session = maybeSession.get();
        if (session.getStatus() != UploadSessionStatus.MERGING) {
            return;
        }

        try {
            mergeParts(uploadId, session);
            session.setStatus(UploadSessionStatus.COMPLETED);
            session.setMergeProgress(100);
            uploadSessionRepository.save(session);
            storageService.deletePartsDirectory(uploadId);
        } catch (IOException e) {
            log.error("Birlestirme basarisiz: uploadId={}", uploadId, e);
            session.setStatus(UploadSessionStatus.FAILED);
            session.setErrorCode("UPLOAD_MERGE_FAILED");
            session.setErrorMessage(e.getMessage());
            uploadSessionRepository.save(session);
        }
    }

    private void mergeParts(UUID uploadId, UploadSessionEntity session) throws IOException {
        long totalChunks = session.getTotalChunks();
        Path finalFile = storageService.resolveMergedFile(uploadId);
        Path tempFile = finalFile.resolveSibling(finalFile.getFileName() + "." + UUID.randomUUID() + ".tmp");

        try {
            writeStreamingWithProgress(uploadId, session, tempFile, totalChunks);
            Files.move(tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    private void writeStreamingWithProgress(UUID uploadId, UploadSessionEntity session, Path tempFile,
                                             long totalChunks) throws IOException {
        Instant lastProgressUpdate = Instant.now();

        try (FileOutputStream output = new FileOutputStream(tempFile.toFile())) {
            for (long i = 0; i < totalChunks; i++) {
                Files.copy(storageService.resolvePartFile(uploadId, i), output);

                Instant now = Instant.now();
                if (Duration.between(lastProgressUpdate, now).toMillis() >= mergeProgressIntervalMs) {
                    int progress = (int) Math.round(((i + 1) * 100.0) / totalChunks);
                    session.setMergeProgress(progress);
                    uploadSessionRepository.save(session);
                    lastProgressUpdate = now;
                }
            }
            output.flush();
            try {
                output.getFD().sync();
            } catch (IOException syncFailure) {
                log.warn("fsync desteklenmiyor veya basarisiz oldu, devam ediliyor: uploadId={}", uploadId);
            }
        }
    }
}