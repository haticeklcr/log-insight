package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.exception.InvalidAnalysisNameException;
import com.hatice.loginsight.exception.InvalidJobStateException;
import com.hatice.loginsight.exception.JobNotFoundException;
import com.hatice.loginsight.exception.JobRetryLimitExceededException;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AnalysisJobLifecycleTest extends AbstractIntegrationTest {

    private static final String SAMPLE_LOG_CONTENT =
            "2026-01-01 10:00:00 INFO Application started\n" +
            "2026-01-01 10:00:01 WARN Deprecated config key used\n" +
            "2026-01-01 10:00:02 ERROR: Connection refused\n" +
            "2026-01-01 10:00:03 ERROR: Connection refused\n";

    @Autowired
    private AnalysisJobService analysisJobService;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    @Autowired
    private TempFileStorageService tempFileStorageService;

    @Autowired
    private AnalysisJobRunner analysisJobRunner;

    @BeforeEach
    void cleanUp() {
        analysisJobRepository.deleteAllInBatch();
        logAnalysisRepository.deleteAllInBatch();
    }

    private MockMultipartFile sampleFile() {
        return new MockMultipartFile(
                "file", "application.log", "text/plain",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private AnalysisJobEntity awaitStatus(UUID jobId, JobStatus expectedStatus) {
        return awaitCondition(() -> analysisJobRepository.findById(jobId).orElseThrow(),
                job -> job.getStatus() == expectedStatus);
    }

    private <T> T awaitCondition(Supplier<T> supplier, java.util.function.Predicate<T> condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        T last = null;
        while (Instant.now().isBefore(deadline)) {
            last = supplier.get();
            if (condition.test(last)) {
                return last;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return last;
    }

    @Test
    void createsJobWithGivenAnalysisName() {
        AnalysisJobEntity job = analysisJobService.createJob(sampleFile(), "Test Analizi");

        assertThat(job.getAnalysisName()).isEqualTo("Test Analizi");
        assertThat(job.getFileName()).isEqualTo("application.log");
    }

    @Test
    void rejectsBlankAnalysisName() {
        assertThrows(InvalidAnalysisNameException.class,
                () -> analysisJobService.createJob(sampleFile(), "   "));
    }

    @Test
    void rejectsAnalysisNameShorterThanThreeCharacters() {
        assertThrows(InvalidAnalysisNameException.class,
                () -> analysisJobService.createJob(sampleFile(), "ab"));
    }

    @Test
    void rejectsAnalysisNameLongerThanHundredCharacters() {
        String tooLong = "a".repeat(101);
        assertThrows(InvalidAnalysisNameException.class,
                () -> analysisJobService.createJob(sampleFile(), tooLong));
    }

    @Test
    void trimsAnalysisNameBeforeSaving() {
        AnalysisJobEntity job = analysisJobService.createJob(sampleFile(), "  Test Analizi  ");

        assertThat(job.getAnalysisName()).isEqualTo("Test Analizi");
    }

    @Test
    void jobStartsInPendingStatus() {
        AnalysisJobEntity job = analysisJobService.createJob(sampleFile(), "Test Analizi");

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void jobEventuallyReachesRunningStatus() {
        AnalysisJobEntity job = analysisJobService.createJob(sampleFile(), "Test Analizi");

        AnalysisJobEntity observed = awaitCondition(
                () -> analysisJobRepository.findById(job.getId()).orElseThrow(),
                j -> j.getStatus() == JobStatus.RUNNING || j.getStatus() == JobStatus.SUCCEEDED);

        assertThat(observed.getStatus()).isIn(JobStatus.RUNNING, JobStatus.SUCCEEDED);
    }

    @Test
    void successfulJobReachesSucceededWithFullProgress() {
        AnalysisJobEntity job = analysisJobService.createJob(sampleFile(), "Test Analizi");

        AnalysisJobEntity finished = awaitStatus(job.getId(), JobStatus.SUCCEEDED);

        assertThat(finished.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(finished.getProgress()).isEqualTo(100);
    }

    @Test
    void successfulJobCreatesLogAnalysisRecord() {
        AnalysisJobEntity job = analysisJobService.createJob(sampleFile(), "Test Analizi");

        AnalysisJobEntity finished = awaitStatus(job.getId(), JobStatus.SUCCEEDED);

        assertThat(finished.getAnalysisId()).isNotNull();
        assertThat(logAnalysisRepository.findById(finished.getAnalysisId())).isPresent();
        assertThat(logAnalysisRepository.findById(finished.getAnalysisId()).get().getAnalysisName())
                .isEqualTo("Test Analizi");
    }

    @Test
    void pendingJobCanBeCancelledImmediately() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Manuel Test");
        job.setFileName("manual.log");
        job.setFileSize(10);
        job.setStatus(JobStatus.PENDING);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        AnalysisJobEntity cancelled = analysisJobService.cancelJob(saved.getId());

        assertThat(cancelled.getStatus()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void succeededJobCannotBeCancelled() {
        AnalysisJobEntity job = analysisJobService.createJob(sampleFile(), "Test Analizi");
        awaitStatus(job.getId(), JobStatus.SUCCEEDED);

        assertThrows(InvalidJobStateException.class, () -> analysisJobService.cancelJob(job.getId()));
    }

    @Test
    void cancellingUnknownJobReturnsNotFound() {
        assertThrows(JobNotFoundException.class, () -> analysisJobService.cancelJob(UUID.randomUUID()));
    }

    @Test
    void cancelledJobDoesNotCreateAnalysisRecord() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Manuel Test");
        job.setFileName("manual.log");
        job.setFileSize(10);
        job.setStatus(JobStatus.PENDING);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        analysisJobService.cancelJob(saved.getId());

        assertThat(saved.getAnalysisId()).isNull();
        assertThat(logAnalysisRepository.count()).isZero();
    }

    @Test
    void failedJobCanBeRetried() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "application.log", "text/plain", new byte[0]);

        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Retry Testi");
        job.setFileName("application.log");
        job.setFileSize(0);
        job.setStatus(JobStatus.FAILED);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCompletedAt(Instant.now());
        job.setErrorCode("ANALYSIS_UNEXPECTED_ERROR");
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        AnalysisJobEntity retried = analysisJobService.retryJob(saved.getId());

        assertThat(retried.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(retried.getRetryCount()).isEqualTo(1);
        assertThat(retried.getErrorCode()).isNull();
    }

    @Test
    void pendingJobCannotBeRetried() {
        AnalysisJobEntity job = analysisJobService.createJob(sampleFile(), "Test Analizi");

        assertThrows(InvalidJobStateException.class, () -> analysisJobService.retryJob(job.getId()));
    }

    @Test
    void retryLimitIsEnforced() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Limit Testi");
        job.setFileName("application.log");
        job.setFileSize(10);
        job.setStatus(JobStatus.FAILED);
        job.setProgress(0);
        job.setRetryCount(3);
        job.setCreatedAt(Instant.now());
        job.setCompletedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        assertThrows(JobRetryLimitExceededException.class, () -> analysisJobService.retryJob(saved.getId()));
    }

    @Test
    void retryLimitExceededDeletesTempFile() throws Exception {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Limit Temizlik Testi");
        job.setFileName("application.log");
        job.setFileSize(10);
        job.setStatus(JobStatus.FAILED);
        job.setProgress(0);
        job.setRetryCount(3);
        job.setCreatedAt(Instant.now());
        job.setCompletedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        tempFileStorageService.store(saved.getId(), sampleFile());
        assertThat(tempFileStorageService.exists(saved.getId())).isTrue();

        assertThrows(JobRetryLimitExceededException.class, () -> analysisJobService.retryJob(saved.getId()));

        assertThat(tempFileStorageService.exists(saved.getId())).isFalse();
    }

    @Test
    void listJobsSupportsPagination() {
        analysisJobService.createJob(sampleFile(), "Analiz Bir");
        analysisJobService.createJob(sampleFile(), "Analiz Iki");
        analysisJobService.createJob(sampleFile(), "Analiz Uc");

        var page = analysisJobService.listJobs(null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    

    @Test
    void realAnalysisFailureDoesNotCreateAnalysisRecord() throws Exception {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Gercek Hata Testi");
        job.setFileName("bozuk.log");
        job.setFileSize(10);
        job.setStatus(JobStatus.RUNNING);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setStartedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        // Dosya diskte hiç yok (hiç store edilmedi) — analiz sırasında NoSuchFileException
        // fırlaması bekleniyor, bu da gerçek bir "RUNNING -> FAILED" senaryosu.
        analysisJobRunner.runAnalysis(saved.getId());

        AnalysisJobEntity failed = awaitStatus(saved.getId(), JobStatus.FAILED);

        assertThat(failed.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(failed.getErrorCode()).isEqualTo("ANALYSIS_SOURCE_FILE_NO_LONGER_AVAILABLE");
        assertThat(failed.getAnalysisId()).isNull();
        assertThat(logAnalysisRepository.count()).isZero();
    }

    @Test
    void successfulJobDeletesItsTempFile() {
        AnalysisJobEntity job = analysisJobService.createJob(sampleFile(), "Temizlik Testi");

        AnalysisJobEntity finished = awaitStatus(job.getId(), JobStatus.SUCCEEDED);

        assertThat(finished.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(tempFileStorageService.exists(job.getId())).isFalse();
    }

    @Test
    void listJobsFiltersByStatus() {
        AnalysisJobEntity pendingJob = new AnalysisJobEntity();
        pendingJob.setAnalysisName("Bekleyen Iş");
        pendingJob.setFileName("a.log");
        pendingJob.setFileSize(1);
        pendingJob.setStatus(JobStatus.PENDING);
        pendingJob.setProgress(0);
        pendingJob.setRetryCount(0);
        pendingJob.setCreatedAt(Instant.now());
        pendingJob.setCancelRequested(false);
        analysisJobRepository.save(pendingJob);

        analysisJobService.createJob(sampleFile(), "Tamamlanacak Iş");
        awaitCondition(() -> analysisJobRepository.count(), count -> count == 2);

        var page = analysisJobService.listJobs(null, null, JobStatus.PENDING,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getAnalysisName()).isEqualTo("Bekleyen Iş");
    }
}