package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.entity.UploadSessionStatus;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.UploadSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StartupRecoveryServiceUploadTest extends AbstractIntegrationTest {

    @Autowired
    private UploadSessionService uploadSessionService;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private StartupRecoveryService startupRecoveryService;

    @AfterEach
    void cleanUp() {
        analysisJobRepository.deleteAllInBatch();
        uploadSessionRepository.deleteAllInBatch();
    }

    private UploadSessionEntity waitForStatus(UUID uploadId, UploadSessionStatus expected) {
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
    void reMergesSessionStuckInMergingWithPartsStillPresent() {
        UploadSessionEntity session = uploadSessionService.createSession("crash-during-merge.log", 1024);
        uploadSessionService.uploadChunk(session.getId(), 0, new byte[1024]);

        // Gercek bir crash simule etmek icin: durumu dogrudan MERGING'e cekiyoruz,
        // gercek UploadMergeRunner.runMerge() hic tetiklenmedi, yani data.log henuz yok
        // ama parcalar hala diskte — spec'in "MERGING, parcalar var" satiri.
        session.setStatus(UploadSessionStatus.MERGING);
        uploadSessionRepository.save(session);

        startupRecoveryService.recoverInterruptedJobs();

        waitForStatus(session.getId(), UploadSessionStatus.COMPLETED);
    }

    @Test
    void marksOrphanedConsumedSessionAsFailedWhenNoJobExists() {
        UploadSessionEntity session = uploadSessionService.createSession("orphaned-consumed.log", 1024);
        uploadSessionService.uploadChunk(session.getId(), 0, new byte[1024]);

        // Gercek bir crash simule etmek icin: "consumeCompletedSession sonrasi, job kaydi
        // olusturulmadan once" ara durumunu dogrudan kuruyoruz.
        session.setStatus(UploadSessionStatus.CONSUMED);
        uploadSessionRepository.save(session);

        startupRecoveryService.recoverInterruptedJobs();

        UploadSessionEntity reloaded = uploadSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UploadSessionStatus.FAILED);
        assertThat(reloaded.getErrorCode()).isEqualTo("UPLOAD_JOB_CREATION_INCOMPLETE");
    }
}