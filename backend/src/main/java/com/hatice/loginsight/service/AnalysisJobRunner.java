package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.FrequentErrorEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnalysisJobRunner {

    private final AnalysisJobRepository analysisJobRepository;
    private final LogAnalysisRepository logAnalysisRepository;
    private final TempFileStorageService tempFileStorageService;
    private final int progressInterval;

    public AnalysisJobRunner(AnalysisJobRepository analysisJobRepository,
                              LogAnalysisRepository logAnalysisRepository,
                              TempFileStorageService tempFileStorageService,
                              @Value("${app.analysis-job.progress-interval}") int progressInterval) {
        this.analysisJobRepository = analysisJobRepository;
        this.logAnalysisRepository = logAnalysisRepository;
        this.tempFileStorageService = tempFileStorageService;
        this.progressInterval = progressInterval;
    }

    @Async("analysisTaskExecutor")
    public void runAnalysis(UUID jobId) {
        Optional<AnalysisJobEntity> maybeJob = analysisJobRepository.findById(jobId);
        if (maybeJob.isEmpty()) {
            return;
        }
        AnalysisJobEntity job = maybeJob.get();

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job = analysisJobRepository.save(job);

        Path filePath = tempFileStorageService.resolve(jobId);

        try {
            long totalBytes = Files.size(filePath);

            int totalLines = 0;
            int infoCount = 0;
            int warningCount = 0;
            int errorCount = 0;
            int exceptionCount = 0;
            Map<String, Integer> errorMessageCounts = new LinkedHashMap<>();

            try (CountingInputStream countingStream = new CountingInputStream(Files.newInputStream(filePath));
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(countingStream, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    totalLines++;

                    if (line.contains("ERROR")) {
                        errorCount++;
                        int idx = line.indexOf("ERROR");
                        String message = line.substring(idx + "ERROR".length()).trim();
                        if (message.startsWith(":")) {
                            message = message.substring(1).trim();
                        }
                        if (!message.isEmpty()) {
                            errorMessageCounts.merge(message, 1, Integer::sum);
                        }
                    } else if (line.contains("WARN")) {
                        warningCount++;
                    } else if (line.contains("INFO")) {
                        infoCount++;
                    }

                    if (line.contains("Exception")) {
                        exceptionCount++;
                    }

                    if (totalLines % progressInterval == 0) {
                        job = analysisJobRepository.findById(jobId).orElseThrow();
                        if (job.isCancelRequested()) {
                            handleCancellation(job);
                            return;
                        }
                        int progress = totalBytes == 0 ? 100 : (int) Math.min(99, (countingStream.getBytesRead() * 100) / totalBytes);
                        job.setProgress(progress);
                        job = analysisJobRepository.save(job);
                    }
                }
            }

            handleSuccess(job, totalLines, infoCount, warningCount, errorCount, exceptionCount, errorMessageCounts);

        } catch (IOException e) {
            handleFailure(job, "ANALYSIS_IO_ERROR", "Log dosyası okunurken bir hata oluştu: " + e.getMessage());
        } catch (Exception e) {
            handleFailure(job, "ANALYSIS_UNEXPECTED_ERROR", e.getMessage());
        }
    }

    private void handleSuccess(AnalysisJobEntity job, int totalLines, int infoCount, int warningCount,
                                int errorCount, int exceptionCount, Map<String, Integer> errorMessageCounts) {
        LogAnalysisEntity entity = new LogAnalysisEntity();
        entity.setFileName(job.getFileName());
        entity.setFileSize(job.getFileSize());
        entity.setAnalysisName(job.getAnalysisName());
        entity.setTotalLines(totalLines);
        entity.setInfoCount(infoCount);
        entity.setWarningCount(warningCount);
        entity.setErrorCount(errorCount);
        entity.setExceptionCount(exceptionCount);
        entity.setAnalyzedAt(Instant.now());
        entity.setProcessingDurationMs(Instant.now().toEpochMilli() - job.getStartedAt().toEpochMilli());

        errorMessageCounts.forEach((message, count) ->
                entity.addFrequentError(new FrequentErrorEntity(message, count)));

        LogAnalysisEntity savedEntity = logAnalysisRepository.save(entity);

        job.setStatus(JobStatus.SUCCEEDED);
        job.setProgress(100);
        job.setCompletedAt(Instant.now());
        job.setAnalysisId(savedEntity.getId());
        job = analysisJobRepository.save(job);

        tempFileStorageService.delete(job.getId());
    }

    private void handleFailure(AnalysisJobEntity job, String errorCode, String errorMessage) {
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(Instant.now());
        job.setErrorCode(errorCode);
        job.setErrorMessage(errorMessage);
        job = analysisJobRepository.save(job);

        tempFileStorageService.delete(job.getId());
    }

    private void handleCancellation(AnalysisJobEntity job) {
        job.setStatus(JobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        job = analysisJobRepository.save(job);

        tempFileStorageService.delete(job.getId());
    }
}