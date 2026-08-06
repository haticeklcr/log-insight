package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.entity.UploadSessionStatus;
import com.hatice.loginsight.repository.UploadSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UploadMergeCorrectnessTest extends AbstractIntegrationTest {

    @Autowired
    private UploadSessionService uploadSessionService;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Autowired
    private ChunkedUploadStorageService storageService;

    @AfterEach
    void cleanUp() {
        uploadSessionRepository.deleteAllInBatch();
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
        throw new AssertionError("Zaman aşımı: " + expected + " durumu beklenirken");
    }

    @Test
    void mergedFileIsByteForByteIdenticalToSourceAcrossMultipleChunks() throws Exception {
        UploadSessionEntity probe = uploadSessionService.createSession("probe.log", 1);
        int chunkSize = probe.getChunkSize();
        uploadSessionService.cancelSession(probe.getId());

        long fileSize = (long) chunkSize * 2 + 777;
        UploadSessionEntity session = uploadSessionService.createSession("merge-test.log", fileSize);
        assertThat(session.getTotalChunks()).isEqualTo(3);

        byte[] expected = new byte[(int) fileSize];
        new java.util.Random(42).nextBytes(expected);

        for (int i = 0; i < session.getTotalChunks(); i++) {
            int start = i * chunkSize;
            int end = (int) Math.min((long) start + chunkSize, fileSize);
            uploadSessionService.uploadChunk(session.getId(), i, Arrays.copyOfRange(expected, start, end));
        }

        uploadSessionService.completeSession(session.getId());
        waitForStatus(session.getId(), UploadSessionStatus.COMPLETED);

        byte[] actual = Files.readAllBytes(storageService.resolveMergedFile(session.getId()));
        assertThat(actual).isEqualTo(expected);
    }
}