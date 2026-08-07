package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.entity.UploadSessionStatus;
import com.hatice.loginsight.repository.UploadSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UploadCleanupService {

    private static final Logger log = LoggerFactory.getLogger(UploadCleanupService.class);

    private final UploadSessionRepository uploadSessionRepository;
    private final ChunkedUploadStorageService storageService;

    public UploadCleanupService(UploadSessionRepository uploadSessionRepository,
                                 ChunkedUploadStorageService storageService) {
        this.uploadSessionRepository = uploadSessionRepository;
        this.storageService = storageService;
    }

    @Scheduled(fixedDelayString = "${app.upload.cleanup-interval}")
    public void cleanUp() {
        cleanUpExpiredSessions();
        cleanUpOrphanedDirectories();
        cleanUpStrayMergeTempFiles();
    }

    private void cleanUpExpiredSessions() {
        Instant now = Instant.now();
        List<UploadSessionEntity> candidates = uploadSessionRepository.findByStatusIn(
                List.of(UploadSessionStatus.IN_PROGRESS, UploadSessionStatus.COMPLETED));

        for (UploadSessionEntity session : candidates) {
            if (session.getExpiresAt().isBefore(now)) {
                log.warn("Süresi dolmuş yükleme oturumu temizleniyor: uploadId={}, status={}",
                        session.getId(), session.getStatus());
                uploadSessionRepository.delete(session);
                storageService.deleteSessionDirectory(session.getId());
            }
        }
    }

    private void cleanUpOrphanedDirectories() {
        Set<UUID> knownSessionIds = uploadSessionRepository.findAll().stream()
                .map(UploadSessionEntity::getId)
                .collect(Collectors.toSet());

        for (UUID dirId : storageService.listSessionDirectoryIds()) {
            if (!knownSessionIds.contains(dirId)) {
                log.warn("Sahipsiz yükleme dizini temizleniyor: {}", dirId);
                storageService.deleteSessionDirectory(dirId);
            }
        }
    }

    private void cleanUpStrayMergeTempFiles() {
        // MERGING durumundaki oturumlar zaten StartupRecoveryService tarafından ele alınıyor
        // (uygulama her yeniden başladığında). Burada, hâlâ MERGING olmayan (yani aktif bir
        // birleştirme denemesine ait olmayan) her data.log.*.tmp dosyası, geçmişte kalmış,
        // temizlenmemiş bir kalıntıdır.
        Set<UUID> mergingSessionIds = uploadSessionRepository
                .findByStatusIn(List.of(UploadSessionStatus.MERGING)).stream()
                .map(UploadSessionEntity::getId)
                .collect(Collectors.toSet());

        for (UUID sessionId : storageService.listSessionDirectoryIds()) {
            if (mergingSessionIds.contains(sessionId)) {
                continue;
            }
            if (storageService.deleteStrayMergeTempFiles(sessionId) > 0) {
                log.warn("uploadId={} icin kalinti birlestirme gecici dosyasi temizlendi", sessionId);
            }
        }
    }
}