package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.exception.InvalidAnalysisNameException;
import com.hatice.loginsight.exception.InvalidDateRangeException;
import com.hatice.loginsight.exception.InvalidParserTypeException;
import com.hatice.loginsight.exception.JobNotFoundException;
import com.hatice.loginsight.exception.JobRetryLimitExceededException;
import com.hatice.loginsight.parser.LogFormat;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final LogFileValidator logFileValidator;
    private final TempFileStorageService tempFileStorageService;
    private final AnalysisJobRunner analysisJobRunner;
    private final JobStateMachine jobStateMachine;
    private final UploadSessionService uploadSessionService;
    private final ChunkedUploadStorageService chunkedUploadStorageService;
    private final int maxRetry;
    private final AnalysisJobService self;

    public AnalysisJobService(AnalysisJobRepository analysisJobRepository,
                               LogFileValidator logFileValidator,
                               TempFileStorageService tempFileStorageService,
                               AnalysisJobRunner analysisJobRunner,
                               JobStateMachine jobStateMachine,
                               UploadSessionService uploadSessionService,
                               ChunkedUploadStorageService chunkedUploadStorageService,
                               @Value("${app.analysis-job.max-retry}") int maxRetry,
                               @Lazy AnalysisJobService self) {
        this.analysisJobRepository = analysisJobRepository;
        this.logFileValidator = logFileValidator;
        this.tempFileStorageService = tempFileStorageService;
        this.analysisJobRunner = analysisJobRunner;
        this.jobStateMachine = jobStateMachine;
        this.uploadSessionService = uploadSessionService;
        this.chunkedUploadStorageService = chunkedUploadStorageService;
        this.maxRetry = maxRetry;
        this.self = self;
    }

    public AnalysisJobEntity createJob(MultipartFile file, String analysisName) {
        return createJob(file, analysisName, null, null, null, null, null, null, null, null, null, null);
    }

    public AnalysisJobEntity createJob(MultipartFile file, String analysisName, String parserType,
                                        String startTime, String endTime, String levels, String logger,
                                        String thread, String messageContains, String statusCodes,
                                        String httpMethods, String pathContains) {
        logFileValidator.validate(file);

        AnalysisJobEntity job = buildBaseJob(analysisName, parserType, startTime, endTime, levels, logger,
                thread, messageContains, statusCodes, httpMethods, pathContains);
        job.setFileName(file.getOriginalFilename());
        job.setFileSize(file.getSize());

        AnalysisJobEntity savedJob = analysisJobRepository.save(job);

        try {
            tempFileStorageService.store(savedJob.getId(), file);
        } catch (IOException e) {
            throw new UncheckedIOException("Dosya geçici olarak kaydedilirken hata oluştu", e);
        }

        analysisJobRunner.runAnalysis(savedJob.getId());

        return savedJob;
    }

    public AnalysisJobEntity createJobFromUpload(UUID uploadId, String analysisName, String parserType,
                                                  String startTime, String endTime, String levels, String logger,
                                                  String thread, String messageContains, String statusCodes,
                                                  String httpMethods, String pathContains) {
        AnalysisJobEntity job = buildBaseJob(analysisName, parserType, startTime, endTime, levels, logger,
                thread, messageContains, statusCodes, httpMethods, pathContains);

        UploadSessionEntity session = uploadSessionService.consumeCompletedSession(uploadId);

        job.setFileName(session.getFileName());
        job.setFileSize(session.getFileSize());
        job.setUploadSessionId(uploadId);

        AnalysisJobEntity savedJob = analysisJobRepository.save(job);

        Path mergedFile = chunkedUploadStorageService.resolveMergedFile(uploadId);
        try {
            Files.move(mergedFile, tempFileStorageService.resolve(savedJob.getId()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Birleştirilmiş dosya analiz dizinine taşınırken hata oluştu", e);
        }

        analysisJobRunner.runAnalysis(savedJob.getId());

        return savedJob;
    }

    private AnalysisJobEntity buildBaseJob(String analysisName, String parserType, String startTime, String endTime,
                                            String levels, String logger, String thread, String messageContains,
                                            String statusCodes, String httpMethods, String pathContains) {
        String trimmedName = validateAnalysisName(analysisName);

        LogFormat requestedFormat = validateAndParseParserType(parserType);
        Instant parsedStartTime = parseInstantOrNull(startTime);
        Instant parsedEndTime = parseInstantOrNull(endTime);
        validateDateRange(parsedStartTime, parsedEndTime);

        if (requestedFormat != null && requestedFormat != LogFormat.AUTO) {
            AnalysisFilterSupport.validate(
                    requestedFormat,
                    isSet(logger),
                    isSet(thread),
                    isSet(statusCodes),
                    isSet(httpMethods),
                    isSet(pathContains));
        }

        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName(trimmedName);
        job.setStatus(JobStatus.PENDING);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);

        job.setRequestedParserType(requestedFormat == null ? null : requestedFormat.name());
        job.setFilterStartTime(parsedStartTime);
        job.setFilterEndTime(parsedEndTime);
        job.setFilterLevels(blankToNull(levels));
        job.setFilterLogger(blankToNull(logger));
        job.setFilterThread(blankToNull(thread));
        job.setFilterMessageContains(blankToNull(messageContains));
        job.setFilterStatusCodes(blankToNull(statusCodes));
        job.setFilterHttpMethods(blankToNull(httpMethods));
        job.setFilterPathContains(blankToNull(pathContains));

        return job;
    }

    private LogFormat validateAndParseParserType(String parserType) {
        if (parserType == null || parserType.isBlank()) {
            return null;
        }
        try {
            return LogFormat.valueOf(parserType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidParserTypeException("Gecersiz parserType: " + parserType);
        }
    }

    private Instant parseInstantOrNull(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(rawValue.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidDateRangeException("Gecersiz tarih formati: " + rawValue);
        }
    }

    private void validateDateRange(Instant startTime, Instant endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new InvalidDateRangeException("endTime, startTime'dan sonra olmalidir");
        }
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    public AnalysisJobEntity getJob(UUID jobId) {
        return findJobOrThrow(jobId);
    }

    public AnalysisJobEntity cancelJob(UUID jobId) {
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return self.doCancelJob(jobId);
            } catch (ObjectOptimisticLockingFailureException e) {
                // Arka plandaki AnalysisJobRunner tam bu anda aynı satırı (örn. progress
                // güncellemesi için) güncellemiş olabilir. self.doCancelJob çağrısı Spring
                // proxy'si üzerinden geçtiği için her deneme KENDİ, taze transaction'ında
                // çalışır — bir önceki denemenin "kirlenmiş" oturumunu miras almaz.
                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Beklenmeyen durum: cancelJob yeniden deneme döngüsü tamamlanamadı");
    }

    @Transactional
    public AnalysisJobEntity doCancelJob(UUID jobId) {
        AnalysisJobEntity job = findJobOrThrow(jobId);
        jobStateMachine.assertCanBeCancelled(job.getStatus());

        if (job.getStatus() == JobStatus.PENDING) {
            job.setStatus(JobStatus.CANCELLED);
            job.setCompletedAt(Instant.now());
            job = analysisJobRepository.save(job);
            tempFileStorageService.delete(job.getId());
        } else {
            job.setCancelRequested(true);
            job = analysisJobRepository.save(job);
        }

        return job;
    }

    public AnalysisJobEntity retryJob(UUID jobId) {
        AnalysisJobEntity job = findJobOrThrow(jobId);
        jobStateMachine.assertCanBeRetried(job.getStatus());

        if (job.getRetryCount() >= maxRetry) {
            // Retry limiti kesin olarak dolduğu için bu job bir daha asla
            // çalıştırılamayacak — geçici dosyaya artık hiç ihtiyaç kalmıyor.
            tempFileStorageService.delete(job.getId());
            throw new JobRetryLimitExceededException(
                    "Maksimum retry sayısına (" + maxRetry + ") ulaşıldı");
        }

        job.setStatus(JobStatus.PENDING);
        job.setRetryCount(job.getRetryCount() + 1);
        job.setProgress(0);
        job.setCancelRequested(false);
        job.setResumedFromCheckpoint(false);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setErrorCode(null);
        job.setErrorMessage(null);
        job = analysisJobRepository.save(job);

        analysisJobRunner.runAnalysis(job.getId());

        return job;
    }

    public Page<AnalysisJobEntity> listJobs(String analysisName, String fileName, JobStatus status, Pageable pageable) {
        Specification<AnalysisJobEntity> spec = (root, query, cb) -> cb.conjunction();

        Specification<AnalysisJobEntity> nameSpec = AnalysisJobSpecifications.hasAnalysisNameContaining(analysisName);
        if (nameSpec != null) {
            spec = spec.and(nameSpec);
        }

        Specification<AnalysisJobEntity> fileNameSpec = AnalysisJobSpecifications.hasFileNameContaining(fileName);
        if (fileNameSpec != null) {
            spec = spec.and(fileNameSpec);
        }

        Specification<AnalysisJobEntity> statusSpec = AnalysisJobSpecifications.hasStatus(status);
        if (statusSpec != null) {
            spec = spec.and(statusSpec);
        }

        return analysisJobRepository.findAll(spec, pageable);
    }

    private AnalysisJobEntity findJobOrThrow(UUID jobId) {
        return analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job bulunamadı: " + jobId));
    }
}