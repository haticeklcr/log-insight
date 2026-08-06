package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.entity.UploadSessionStatus;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.UploadSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StartupRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(StartupRecoveryService.class);
    private static final String RESTART_ERROR_CODE = "APPLICATION_RESTARTED_DURING_ANALYSIS";

    private final AnalysisJobRepository analysisJobRepository;
    private final TempFileStorageService tempFileStorageService;
    private final UploadSessionRepository uploadSessionRepository;
    private final ChunkedUploadStorageService chunkedUploadStorageService;
    private final UploadMergeRunner uploadMergeRunner;

    public StartupRecoveryService(AnalysisJobRepository analysisJobRepository,
                                   TempFileStorageService tempFileStorageService,
                                   UploadSessionRepository uploadSessionRepository,
                                   ChunkedUploadStorageService chunkedUploadStorageService,
                                   UploadMergeRunner uploadMergeRunner) {
        this.analysisJobRepository = analysisJobRepository;
        this.tempFileStorageService = tempFileStorageService;
        this.uploadSessionRepository = uploadSessionRepository;
        this.chunkedUploadStorageService = chunkedUploadStorageService;
        this.uploadMergeRunner = uploadMergeRunner;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedJobs() {
        recoverRunningJobs();
        cleanUpOrphanedTempFiles();
        recoverUploadSessions();
    }

    private void recoverRunningJobs() {
        List<AnalysisJobEntity> runningJobs = analysisJobRepository.findByStatus(JobStatus.RUNNING);
        if (runningJobs.isEmpty()) {
            return;
        }
        log.warn("Uygulama başlangıcında {} adet yarım kalmış (RUNNING) job tespit edildi, FAILED olarak işaretleniyor",
                runningJobs.size());
        for (AnalysisJobEntity job : runningJobs) {
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorCode(RESTART_ERROR_CODE);
            job.setErrorMessage("Uygulama analiz sürerken yeniden başlatıldı");
            analysisJobRepository.save(job);
            // Not: dosya BURADA silinmiyor — diğer FAILED job'larla aynı kural geçerli:
            // bu job retry edilebileceği için dosyaya ihtiyaç kalabilir.
        }
    }

    private void cleanUpOrphanedTempFiles() {
        Set<UUID> jobIdsExpectingFile = new java.util.HashSet<>();
        analysisJobRepository.findByStatus(JobStatus.PENDING)
                .forEach(job -> jobIdsExpectingFile.add(job.getId()));
        analysisJobRepository.findByStatus(JobStatus.FAILED)
                .forEach(job -> jobIdsExpectingFile.add(job.getId()));
        List<UUID> storedFileIds = tempFileStorageService.listStoredJobIds();
        int deletedCount = 0;
        for (UUID storedId : storedFileIds) {
            if (!jobIdsExpectingFile.contains(storedId)) {
                tempFileStorageService.delete(storedId);
                deletedCount++;
            }
        }
        if (deletedCount > 0) {
            log.warn("Uygulama başlangıcında {} adet sahipsiz geçici dosya temizlendi", deletedCount);
        }
    }

    private void recoverUploadSessions() {
        List<UploadSessionEntity> mergingSessions =
                uploadSessionRepository.findByStatusIn(List.of(UploadSessionStatus.MERGING));
        if (!mergingSessions.isEmpty()) {
            log.warn("Uygulama başlangıcında {} adet MERGING durumunda yükleme oturumu tespit edildi",
                    mergingSessions.size());
        }
        for (UploadSessionEntity session : mergingSessions) {
            recoverMergingSession(session);
        }
        cleanUpOrphanedUploadDirectories();
    }

    private void recoverMergingSession(UploadSessionEntity session) {
        UUID uploadId = session.getId();

        if (chunkedUploadStorageService.mergedFileExists(uploadId)) {
            log.warn("uploadId={} icin birlestirme aslinda tamamlanmis, COMPLETED olarak isaretleniyor", uploadId);
            session.setStatus(UploadSessionStatus.COMPLETED);
            session.setMergeProgress(100);
            uploadSessionRepository.save(session);
            chunkedUploadStorageService.deletePartsDirectory(uploadId);
            return;
        }

        long missingCount = chunkedUploadStorageService
                .findMissingPartIndices(uploadId, session.getTotalChunks()).size();
        if (missingCount < session.getTotalChunks()) {
            log.warn("uploadId={} icin parcalar mevcut, birlestirme yeniden baslatiliyor", uploadId);
            uploadMergeRunner.runMerge(uploadId);
            return;
        }

        log.warn("uploadId={} icin ne birlestirilmis dosya ne parcalar bulundu, FAILED olarak isaretleniyor", uploadId);
        session.setStatus(UploadSessionStatus.FAILED);
        session.setErrorCode("UPLOAD_MERGE_FAILED");
        session.setErrorMessage("Yeniden başlatma sırasında ne birleştirilmiş dosya ne de parçalar bulunamadı");
        uploadSessionRepository.save(session);
    }

    private void cleanUpOrphanedUploadDirectories() {
        Set<UUID> knownSessionIds = uploadSessionRepository.findAll().stream()
                .map(UploadSessionEntity::getId)
                .collect(Collectors.toSet());
        List<UUID> storedDirIds = chunkedUploadStorageService.listSessionDirectoryIds();
        int deletedCount = 0;
        for (UUID storedId : storedDirIds) {
            if (!knownSessionIds.contains(storedId)) {
                chunkedUploadStorageService.deleteSessionDirectory(storedId);
                deletedCount++;
            }
        }
        if (deletedCount > 0) {
            log.warn("Uygulama başlangıcında {} adet sahipsiz yükleme dizini temizlendi", deletedCount);
        }
    }
}