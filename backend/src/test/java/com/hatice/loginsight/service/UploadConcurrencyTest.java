package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.entity.UploadSessionStatus;
import com.hatice.loginsight.exception.TooManyActiveUploadsException;
import com.hatice.loginsight.exception.UploadAlreadyConsumedException;
import com.hatice.loginsight.exception.UploadSessionNotFoundException;
import com.hatice.loginsight.exception.UploadSessionNotInProgressException;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.UploadSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class UploadConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private UploadSessionService uploadSessionService;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private AnalysisJobService analysisJobService;

    @Autowired
    private UploadCleanupService uploadCleanupService;

    @AfterEach
    void cleanUp() {
        analysisJobRepository.deleteAllInBatch();
        uploadSessionRepository.deleteAllInBatch();
    }

    private byte[] contentOfSize(int size, byte fill) {
        byte[] content = new byte[size];
        java.util.Arrays.fill(content, fill);
        return content;
    }

    private UploadSessionEntity newSingleChunkSession() {
        return uploadSessionService.createSession("concurrency-test.log", 1024);
    }

    private UploadSessionEntity waitForStatus(java.util.UUID uploadId, UploadSessionStatus expected) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            UploadSessionEntity session = uploadSessionRepository.findById(uploadId).orElseThrow();
            if (session.getStatus() == expected) {
                return session;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("Zaman aşımı: " + uploadId + " icin " + expected + " durumu beklenirken");
    }

    @Test
    void concurrentUploadOfSameChunkOnlyWritesOnce() throws Exception {
        UploadSessionEntity session = newSingleChunkSession();
        byte[] contentA = contentOfSize(1024, (byte) 'A');
        byte[] contentB = contentOfSize(1024, (byte) 'B');

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            Future<Void> f1 = executor.submit(() -> {
                startLatch.await();
                uploadSessionService.uploadChunk(session.getId(), 0, contentA);
                return null;
            });
            Future<Void> f2 = executor.submit(() -> {
                startLatch.await();
                uploadSessionService.uploadChunk(session.getId(), 0, contentB);
                return null;
            });
            startLatch.countDown();
            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        var status = uploadSessionService.getStatus(session.getId());
        assertThat(status.getReceivedCount()).isEqualTo(1);
        assertThat(status.getMissingChunks()).isEmpty();
    }

    @Test
    void concurrentCompleteRequestsOnlySucceedOnce() throws Exception {
        UploadSessionEntity session = newSingleChunkSession();
        uploadSessionService.uploadChunk(session.getId(), 0, contentOfSize(1024, (byte) 'X'));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    try {
                        uploadSessionService.completeSession(session.getId());
                        successCount.incrementAndGet();
                    } catch (UploadSessionNotInProgressException e) {
                        conflictCount.incrementAndGet();
                    }
                    return null;
                }));
            }
            startLatch.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    @Test
    void uploadingChunkWhileMergingIsRejected() {
        UploadSessionEntity session = newSingleChunkSession();
        session.setStatus(UploadSessionStatus.MERGING);
        uploadSessionRepository.save(session);

        assertThrows(UploadSessionNotInProgressException.class,
                () -> uploadSessionService.uploadChunk(session.getId(), 0, contentOfSize(1024, (byte) 'Z')));
    }

    @Test
    void cancellingSessionRejectsSubsequentChunkUploads() {
        UploadSessionEntity session = newSingleChunkSession();
        uploadSessionService.cancelSession(session.getId());

        assertThrows(UploadSessionNotFoundException.class,
                () -> uploadSessionService.uploadChunk(session.getId(), 0, contentOfSize(1024, (byte) 'Y')));
    }

    @Test
    void onlyOneJobCanBeCreatedFromTheSameUploadId() throws Exception {
        UploadSessionEntity session = newSingleChunkSession();
        uploadSessionService.uploadChunk(session.getId(), 0, contentOfSize(1024, (byte) 'M'));
        uploadSessionService.completeSession(session.getId());
        waitForStatus(session.getId(), UploadSessionStatus.COMPLETED);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int attempt = i;
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    try {
                        analysisJobService.createJobFromUpload(session.getId(), "Yaris Testi " + attempt,
                                null, null, null, null, null, null, null, null, null, null);
                        successCount.incrementAndGet();
                    } catch (UploadAlreadyConsumedException e) {
                        conflictCount.incrementAndGet();
                    }
                    return null;
                }));
            }
            startLatch.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
        assertThat(analysisJobRepository.findByUploadSessionId(session.getId())).isPresent();
    }

    @Test
    void concurrentSessionCreationRespectsActiveSessionLimit() throws Exception {
        int existingActive = uploadSessionRepository.findByStatusIn(
                List.of(UploadSessionStatus.IN_PROGRESS, UploadSessionStatus.MERGING)).size();
        int defaultMaxActiveSessions = 5;
        int remainingSlots = Math.max(0, defaultMaxActiveSessions - existingActive);
        for (int i = 0; i < remainingSlots; i++) {
            uploadSessionService.createSession("dolgu-" + i + ".log", 1024);
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int attempt = i;
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    try {
                        uploadSessionService.createSession("fazla-" + attempt + ".log", 1024);
                    } catch (TooManyActiveUploadsException e) {
                        rejectedCount.incrementAndGet();
                    }
                    return null;
                }));
            }
            startLatch.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(rejectedCount.get()).isEqualTo(2);
    }

    @Test
    void cleanupTaskDoesNotDeleteActiveNonExpiredSession() {
        UploadSessionEntity session = newSingleChunkSession();
        uploadSessionService.uploadChunk(session.getId(), 0, contentOfSize(1024, (byte) 'K'));

        uploadCleanupService.cleanUp();

        assertThat(uploadSessionRepository.findById(session.getId())).isPresent();
        assertThat(uploadSessionService.getStatus(session.getId()).getMissingChunks()).isEmpty();
    }
}