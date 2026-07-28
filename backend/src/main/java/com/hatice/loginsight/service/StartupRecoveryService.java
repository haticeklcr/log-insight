package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.repository.AnalysisJobRepository;
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

    public StartupRecoveryService(AnalysisJobRepository analysisJobRepository,
                                   TempFileStorageService tempFileStorageService) {
        this.analysisJobRepository = analysisJobRepository;
        this.tempFileStorageService = tempFileStorageService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedJobs() {
        recoverRunningJobs();
        cleanUpOrphanedTempFiles();
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
}