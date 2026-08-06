package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.repository.UploadSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UploadCleanupServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UploadSessionService uploadSessionService;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Autowired
    private UploadCleanupService uploadCleanupService;

    @AfterEach
    void cleanUp() {
        uploadSessionRepository.deleteAllInBatch();
    }

    @Test
    void deletesExpiredIncompleteSession() {
        UploadSessionEntity session = uploadSessionService.createSession("expired.log", 1024);
        session.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        uploadSessionRepository.save(session);

        uploadCleanupService.cleanUp();

        assertThat(uploadSessionRepository.findById(session.getId())).isEmpty();
    }

    @Test
    void doesNotDeleteSessionThatHasNotExpiredYet() {
        UploadSessionEntity session = uploadSessionService.createSession("active.log", 1024);

        uploadCleanupService.cleanUp();

        assertThat(uploadSessionRepository.findById(session.getId())).isPresent();
    }
}