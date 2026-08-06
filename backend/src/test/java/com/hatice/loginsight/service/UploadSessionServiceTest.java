package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.entity.UploadSessionStatus;
import com.hatice.loginsight.exception.InvalidChunkIndexException;
import com.hatice.loginsight.exception.InvalidChunkSizeException;
import com.hatice.loginsight.exception.InvalidUploadRequestException;
import com.hatice.loginsight.exception.UnsupportedFileTypeException;
import com.hatice.loginsight.exception.UploadIncompleteException;
import com.hatice.loginsight.exception.UploadSessionExpiredException;
import com.hatice.loginsight.repository.UploadSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class UploadSessionServiceTest extends AbstractIntegrationTest {

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

    @Test
    void createsSessionWithCorrectTotalChunksAndStatus() {
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 1024);

        assertThat(session.getStatus()).isEqualTo(UploadSessionStatus.IN_PROGRESS);
        assertThat(session.getTotalChunks()).isEqualTo(1);
        assertThat(session.getMergeProgress()).isZero();
    }

    @Test
    void rejectsUnsupportedFileExtension() {
        assertThrows(UnsupportedFileTypeException.class,
                () -> uploadSessionService.createSession("test.exe", 1024));
    }

    @Test
    void rejectsNonPositiveFileSize() {
        assertThrows(InvalidUploadRequestException.class,
                () -> uploadSessionService.createSession("test.log", 0));
        assertThrows(InvalidUploadRequestException.class,
                () -> uploadSessionService.createSession("test.log", -5));
    }

    @Test
    void uploadsSingleChunkSuccessfully() {
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 1024);

        assertDoesNotThrow(() -> uploadSessionService.uploadChunk(session.getId(), 0, new byte[1024]));

        var status = uploadSessionService.getStatus(session.getId());
        assertThat(status.getReceivedCount()).isEqualTo(1);
        assertThat(status.getMissingChunks()).isEmpty();
    }

    @Test
    void rejectsInvalidChunkIndex() {
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 1024);

        assertThrows(InvalidChunkIndexException.class,
                () -> uploadSessionService.uploadChunk(session.getId(), -1, new byte[1024]));
        assertThrows(InvalidChunkIndexException.class,
                () -> uploadSessionService.uploadChunk(session.getId(), 1, new byte[1024]));
    }

    @Test
    void rejectsChunkWithWrongSize() {
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 1024);

        assertThrows(InvalidChunkSizeException.class,
                () -> uploadSessionService.uploadChunk(session.getId(), 0, new byte[500]));
    }

    @Test
    void reuploadingSameSizeChunkDoesNotThrowAndPreservesContent() {
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 4);
        byte[] first = {1, 2, 3, 4};
        byte[] second = {9, 9, 9, 9};

        uploadSessionService.uploadChunk(session.getId(), 0, first);
        assertDoesNotThrow(() -> uploadSessionService.uploadChunk(session.getId(), 0, second));

        var status = uploadSessionService.getStatus(session.getId());
        assertThat(status.getReceivedCount()).isEqualTo(1);
    }

    @Test
    void reuploadingDifferentSizeChunkAtStorageLevelIsDetected() {
        // Not: UploadSessionService.uploadChunk() bu senaryoyu servis katmaninda hic
        // ChunkedUploadStorageService'e ulastirmiyor — bir session icin bir chunk index'in
        // beklenen boyutu sabittir, o yuzden bu, depolama katmaninin KENDI sozlesmesini
        // (ayni boyut->204 sessiz, farkli boyut->409) dogrudan test ediyor.
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 8);

        ChunkedUploadStorageService.ChunkWriteResult first =
                storageService.writePart(session.getId(), 0, new byte[8]);
        ChunkedUploadStorageService.ChunkWriteResult second =
                storageService.writePart(session.getId(), 0, new byte[4]);

        assertThat(first).isEqualTo(ChunkedUploadStorageService.ChunkWriteResult.WRITTEN);
        assertThat(second).isEqualTo(ChunkedUploadStorageService.ChunkWriteResult.ALREADY_EXISTS_DIFFERENT_SIZE);
    }

    @Test
    void expiredSessionRejectsChunkUpload() {
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 1024);
        session.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        uploadSessionRepository.save(session);

        assertThrows(UploadSessionExpiredException.class,
                () -> uploadSessionService.uploadChunk(session.getId(), 0, new byte[1024]));
    }

    @Test
    void missingChunksAreComputedCorrectly() {
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 100);

        var beforeStatus = uploadSessionService.getStatus(session.getId());
        assertThat(beforeStatus.getMissingChunks()).containsExactly(0L);
        assertThat(beforeStatus.getReceivedCount()).isZero();

        uploadSessionService.uploadChunk(session.getId(), 0, new byte[100]);

        var afterStatus = uploadSessionService.getStatus(session.getId());
        assertThat(afterStatus.getMissingChunks()).isEmpty();
        assertThat(afterStatus.getReceivedCount()).isEqualTo(1);
    }

    @Test
    void missingChunksAreComputedCorrectlyAcrossMultipleChunks() {
        // Sunucunun gerçek parça boyutunu öğrenip ona göre 2 parçalı bir dosya kur —
        // böylece UPLOAD_CHUNK_SIZE değişse bile test doğru kalır.
        UploadSessionEntity probe = uploadSessionService.createSession("probe.log", 1);
        int chunkSize = probe.getChunkSize();
        uploadSessionService.cancelSession(probe.getId());

        long fileSize = (long) chunkSize + 100;
        UploadSessionEntity session = uploadSessionService.createSession("multi.log", fileSize);
        assertThat(session.getTotalChunks()).isEqualTo(2);

        uploadSessionService.uploadChunk(session.getId(), 1, new byte[100]);

        var status = uploadSessionService.getStatus(session.getId());
        assertThat(status.getMissingChunks()).containsExactly(0L);
        assertThat(status.getReceivedCount()).isEqualTo(1);
    }

    @Test
    void completingWithMissingChunksIsRejected() {
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 200);

        assertThrows(UploadIncompleteException.class,
                () -> uploadSessionService.completeSession(session.getId()));
    }

    @Test
    void cancellingSessionRemovesItFromRepository() {
        UploadSessionEntity session = uploadSessionService.createSession("test.log", 1024);

        uploadSessionService.cancelSession(session.getId());

        assertThat(uploadSessionRepository.findById(session.getId())).isEmpty();
    }
}