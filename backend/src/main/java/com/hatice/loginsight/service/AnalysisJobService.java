package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.exception.InvalidAnalysisNameException;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;

@Service
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final LogFileValidator logFileValidator;
    private final TempFileStorageService tempFileStorageService;
    private final AnalysisJobRunner analysisJobRunner;

    public AnalysisJobService(AnalysisJobRepository analysisJobRepository,
                               LogFileValidator logFileValidator,
                               TempFileStorageService tempFileStorageService,
                               AnalysisJobRunner analysisJobRunner) {
        this.analysisJobRepository = analysisJobRepository;
        this.logFileValidator = logFileValidator;
        this.tempFileStorageService = tempFileStorageService;
        this.analysisJobRunner = analysisJobRunner;
    }

    public AnalysisJobEntity createJob(MultipartFile file, String analysisName) {
        logFileValidator.validate(file);
        String trimmedName = validateAnalysisName(analysisName);

        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName(trimmedName);
        job.setFileName(file.getOriginalFilename());
        job.setFileSize(file.getSize());
        job.setStatus(JobStatus.PENDING);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);

        AnalysisJobEntity savedJob = analysisJobRepository.save(job);

        try {
            tempFileStorageService.store(savedJob.getId(), file);
        } catch (IOException e) {
            throw new UncheckedIOException("Dosya geçici olarak kaydedilirken hata oluştu", e);
        }

        analysisJobRunner.runAnalysis(savedJob.getId());

        return savedJob;
    }

    private String validateAnalysisName(String analysisName) {
        if (analysisName == null) {
            throw new InvalidAnalysisNameException("Analiz adı zorunludur");
        }
        String trimmed = analysisName.trim();
        if (trimmed.length() < 3 || trimmed.length() > 100) {
            throw new InvalidAnalysisNameException("Analiz adı 3 ile 100 karakter arasında olmalıdır");
        }
        return trimmed;
    }
}