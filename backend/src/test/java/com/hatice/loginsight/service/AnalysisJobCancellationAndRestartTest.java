package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "app.analysis-job.progress-interval-ms=10")
class AnalysisJobCancellationAndRestartTest extends AbstractIntegrationTest {

    @Autowired
    private AnalysisJobService analysisJobService;

    @Autowired
    private AnalysisJobRunner analysisJobRunner;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    @Autowired
    private StartupRecoveryService startupRecoveryService;

    @Autowired
    private TempFileStorageService tempFileStorageService;

    @BeforeEach
    void cleanUp() {
        analysisJobRepository.deleteAllInBatch();
        logAnalysisRepository.deleteAllInBatch();
    }

    private MockMultipartFile largeLogFile() {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            content.append("2026-01-01 10:00:00 INFO Processing item ").append(i).append("\n");
        }
        return new MockMultipartFile(
                "file", "large.log", "text/plain",
                content.toString().getBytes(StandardCharsets.UTF_8));
    }

    private <T> T awaitCondition(Supplier<T> supplier, Predicate<T> condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        T last = null;
        while (Instant.now().isBefore(deadline)) {
            last = supplier.get();
            if (condition.test(last)) {
                return last;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return last;
    }

    @Test
    void progressIncreasesWhileJobIsRunning() {
        AnalysisJobEntity job = analysisJobService.createJob(largeLogFile(), "Progress Testi");

        AnalysisJobEntity midProgress = awaitCondition(
                () -> analysisJobRepository.findById(job.getId()).orElseThrow(),
                j -> j.getProgress() > 0);

        assertThat(midProgress.getProgress()).isGreaterThan(0);

        AnalysisJobEntity finished = awaitCondition(
                () -> analysisJobRepository.findById(job.getId()).orElseThrow(),
                j -> j.getStatus() == JobStatus.SUCCEEDED);

        assertThat(finished.getProgress()).isEqualTo(100);
    }

    @Test
    void runningJobStopsAfterCancelRequest() {
        AnalysisJobEntity job = analysisJobService.createJob(largeLogFile(), "Iptal Testi");

        awaitCondition(
                () -> analysisJobRepository.findById(job.getId()).orElseThrow(),
                j -> j.getStatus() == JobStatus.RUNNING);

        analysisJobService.cancelJob(job.getId());

        AnalysisJobEntity cancelled = awaitCondition(
                () -> analysisJobRepository.findById(job.getId()).orElseThrow(),
                j -> j.getStatus() == JobStatus.CANCELLED);

        assertThat(cancelled.getStatus()).isEqualTo(JobStatus.CANCELLED);
        assertThat(cancelled.getAnalysisId()).isNull();
        assertThat(logAnalysisRepository.count()).isZero();
    }

    @Test
    void retryingAlreadyPendingJobDoesNotStartSecondExecution() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Cift Calistirma Testi");
        job.setFileName("application.log");
        job.setFileSize(10);
        job.setStatus(JobStatus.FAILED);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCompletedAt(Instant.now());
        job.setErrorCode("ANALYSIS_UNEXPECTED_ERROR");
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        AnalysisJobEntity firstRetry = analysisJobService.retryJob(saved.getId());
        assertThat(firstRetry.getRetryCount()).isEqualTo(1);
        assertThat(firstRetry.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void startupRecoveryMarksRunningJobsAsFailed() {
        AnalysisJobEntity stuckJob = new AnalysisJobEntity();
        stuckJob.setAnalysisName("Yarim Kalan Is");
        stuckJob.setFileName("stuck.log");
        stuckJob.setFileSize(10);
        stuckJob.setStatus(JobStatus.RUNNING);
        stuckJob.setProgress(42);
        stuckJob.setRetryCount(0);
        stuckJob.setCreatedAt(Instant.now());
        stuckJob.setStartedAt(Instant.now());
        stuckJob.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(stuckJob);

        startupRecoveryService.recoverInterruptedJobs();

        AnalysisJobEntity recovered = analysisJobRepository.findById(saved.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(recovered.getErrorCode()).isEqualTo("APPLICATION_RESTARTED_DURING_ANALYSIS");
    }

    @Test
    void startupRecoveryCleansUpOrphanedTempFile() throws Exception {
        UUID orphanId = UUID.randomUUID();
        tempFileStorageService.store(orphanId,
                new MockMultipartFile("file", "orphan.log", "text/plain", "test".getBytes(StandardCharsets.UTF_8)));
        assertThat(tempFileStorageService.exists(orphanId)).isTrue();

        startupRecoveryService.recoverInterruptedJobs();

        assertThat(tempFileStorageService.exists(orphanId)).isFalse();
    }
}