package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.parser.ParsedLogEntry;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "app.analysis-job.progress-interval-ms=10")
class AnalysisCheckpointResumeTest extends AbstractIntegrationTest {

    @Autowired
    private AnalysisJobRunner analysisJobRunner;

    @Autowired
    private AnalysisJobService analysisJobService;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    @Autowired
    private TempFileStorageService tempFileStorageService;

    @Autowired
    private AnalysisCheckpointService analysisCheckpointService;

    @Autowired
    private com.hatice.loginsight.repository.AnalysisJobCheckpointRepository checkpointRepository;

    @Autowired
    private StartupRecoveryService startupRecoveryService;

    @AfterEach
    void cleanUp() {
        analysisJobRepository.deleteAllInBatch();
        logAnalysisRepository.deleteAllInBatch();
    }

    private <T> T awaitCondition(java.util.function.Supplier<T> supplier, java.util.function.Predicate<T> condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        T last = null;
        while (Instant.now().isBefore(deadline)) {
            last = supplier.get();
            if (condition.test(last)) {
                return last;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return last;
    }

    @Test
    void resumesFromCheckpointWithoutReprocessingSkippedLines() throws Exception {
        String fileContent = "2026-01-01 10:00:00 INFO first\n"
                + "2026-01-01 10:00:01 INFO second\n"
                + "2026-01-01 10:00:02 ERROR third\n"
                + "2026-01-01 10:00:03 WARN fourth\n";
        byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

        long resumeOffset;
        Path probePath = Files.createTempFile("checkpoint-probe", ".log");
        try {
            Files.write(probePath, fileBytes);
            try (ByteOffsetLineReader probe = new ByteOffsetLineReader(probePath)) {
                probe.readLine();
                probe.readLine();
                resumeOffset = probe.getCurrentPosition();
            }
        } finally {
            Files.deleteIfExists(probePath);
        }

        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Checkpoint Devam Testi");
        job.setFileName("resume.log");
        job.setFileSize(fileBytes.length);
        job.setStatus(JobStatus.FAILED);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        tempFileStorageService.store(saved.getId(),
                new MockMultipartFile("file", "resume.log", "text/plain", fileBytes));

        AnalysisResultAccumulator baselineAccumulator = new AnalysisResultAccumulator(200, 500);
        ParsedLogEntry first = new ParsedLogEntry();
        first.setTimestamp(Instant.parse("2026-01-01T10:00:00Z"));
        first.setLevel("INFO");
        first.setMessage("first");
        baselineAccumulator.incrementTotalLines();
        baselineAccumulator.recordEntry(first, null);
        ParsedLogEntry second = new ParsedLogEntry();
        second.setTimestamp(Instant.parse("2026-01-01T10:00:01Z"));
        second.setLevel("INFO");
        second.setMessage("second");
        baselineAccumulator.incrementTotalLines();
        baselineAccumulator.recordEntry(second, null);

        analysisCheckpointService.saveCheckpoint(saved.getId(), resumeOffset, baselineAccumulator);

        analysisJobRunner.runAnalysis(saved.getId());

        AnalysisJobEntity finished = awaitCondition(
                () -> analysisJobRepository.findById(saved.getId()).orElseThrow(),
                j -> j.getStatus() == JobStatus.SUCCEEDED);

        assertThat(finished.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(finished.isResumedFromCheckpoint()).isTrue();

        LogAnalysisEntity result = logAnalysisRepository.findById(finished.getAnalysisId()).orElseThrow();
        assertThat(result.getTotalLines()).isEqualTo(4);
        assertThat(result.getInfoCount()).isEqualTo(2);
        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getWarningCount()).isEqualTo(1);
    }

    @Test
    void freshRunWithoutCheckpointIsNotMarkedAsResumed() throws Exception {
        String fileContent = "2026-01-01 10:00:00 INFO tek satir\n";
        byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Checkpointsiz Test");
        job.setFileName("fresh.log");
        job.setFileSize(fileBytes.length);
        job.setStatus(JobStatus.PENDING);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        tempFileStorageService.store(saved.getId(),
                new MockMultipartFile("file", "fresh.log", "text/plain", fileBytes));

        analysisJobRunner.runAnalysis(saved.getId());

        AnalysisJobEntity finished = awaitCondition(
                () -> analysisJobRepository.findById(saved.getId()).orElseThrow(),
                j -> j.getStatus() == JobStatus.SUCCEEDED);

        assertThat(finished.isResumedFromCheckpoint()).isFalse();
    }

    @Test
    void nonResumableFailureDiscardsCheckpoint() throws Exception {
        String fileContent = "2026-01-01 10:00:00 INFO first\n2026-01-01 10:00:01 INFO second\n";
        byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Kalici Olmayan Hata Testi");
        job.setFileName("bad-parser.log");
        job.setFileSize(fileBytes.length);
        job.setStatus(JobStatus.PENDING);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);
        job.setRequestedParserType("JSON");
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        tempFileStorageService.store(saved.getId(),
                new MockMultipartFile("file", "bad-parser.log", "text/plain", fileBytes));

        AnalysisResultAccumulator baselineAccumulator = new AnalysisResultAccumulator(200, 500);
        analysisCheckpointService.saveCheckpoint(saved.getId(), 0, baselineAccumulator);

        analysisJobRunner.runAnalysis(saved.getId());

        AnalysisJobEntity finished = awaitCondition(
                () -> analysisJobRepository.findById(saved.getId()).orElseThrow(),
                j -> j.getStatus() == JobStatus.FAILED);

        assertThat(finished.getErrorCode()).isEqualTo("SELECTED_PARSER_CANNOT_PARSE_FILE");
        assertThat(analysisCheckpointService.loadCheckpoint(saved.getId())).isEmpty();
    }

    @Test
    void resumableIoErrorPreservesCheckpoint() throws Exception {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Gecici IO Hatasi Testi");
        job.setFileName("vanished.log");
        job.setFileSize(10);
        job.setStatus(JobStatus.PENDING);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        // Gercek dosya yerine bir DIZIN koyarak, dosya acilirken kesin bir IOException olusmasini sagliyoruz.
        Path tempPath = tempFileStorageService.resolve(saved.getId());
        Files.createDirectories(tempPath);

        AnalysisResultAccumulator baselineAccumulator = new AnalysisResultAccumulator(200, 500);
        analysisCheckpointService.saveCheckpoint(saved.getId(), 0, baselineAccumulator);

        analysisJobRunner.runAnalysis(saved.getId());

        AnalysisJobEntity finished = awaitCondition(
                () -> analysisJobRepository.findById(saved.getId()).orElseThrow(),
                j -> j.getStatus() == JobStatus.FAILED);

        assertThat(finished.getErrorCode()).isEqualTo("ANALYSIS_IO_ERROR");
        assertThat(analysisCheckpointService.loadCheckpoint(saved.getId())).isPresent();

        Files.deleteIfExists(tempPath);
    }

    @Test
    void checkpointSurvivesRestartRecoveryAndIsUsedOnRetry() throws Exception {
        String fileContent = "2026-01-01 10:00:00 INFO first\n"
                + "2026-01-01 10:00:01 INFO second\n"
                + "2026-01-01 10:00:02 ERROR third\n";
        byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

        long resumeOffset;
        Path probePath = Files.createTempFile("restart-probe", ".log");
        try {
            Files.write(probePath, fileBytes);
            try (ByteOffsetLineReader probe = new ByteOffsetLineReader(probePath)) {
                probe.readLine();
                resumeOffset = probe.getCurrentPosition();
            }
        } finally {
            Files.deleteIfExists(probePath);
        }

        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Restart Sonrasi Devam Testi");
        job.setFileName("restart.log");
        job.setFileSize(fileBytes.length);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job.setProgress(30);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        tempFileStorageService.store(saved.getId(),
                new MockMultipartFile("file", "restart.log", "text/plain", fileBytes));

        AnalysisResultAccumulator baselineAccumulator = new AnalysisResultAccumulator(200, 500);
        ParsedLogEntry first = new ParsedLogEntry();
        first.setTimestamp(Instant.parse("2026-01-01T10:00:00Z"));
        first.setLevel("INFO");
        first.setMessage("first");
        baselineAccumulator.incrementTotalLines();
        baselineAccumulator.recordEntry(first, null);
        analysisCheckpointService.saveCheckpoint(saved.getId(), resumeOffset, baselineAccumulator);

        // Uygulama yeniden basladiginda calisan kurtarma — RUNNING job'u FAILED yapiyor
        // ama checkpoint'e HIC dokunmuyor (bilincli tasarim).
        startupRecoveryService.recoverInterruptedJobs();

        AnalysisJobEntity afterRestart = analysisJobRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterRestart.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(afterRestart.getErrorCode()).isEqualTo("APPLICATION_RESTARTED_DURING_ANALYSIS");
        assertThat(analysisCheckpointService.loadCheckpoint(saved.getId())).isPresent();

        analysisJobService.retryJob(saved.getId());

        AnalysisJobEntity finished = awaitCondition(
                () -> analysisJobRepository.findById(saved.getId()).orElseThrow(),
                j -> j.getStatus() == JobStatus.SUCCEEDED);

        assertThat(finished.isResumedFromCheckpoint()).isTrue();
        LogAnalysisEntity result = logAnalysisRepository.findById(finished.getAnalysisId()).orElseThrow();
        assertThat(result.getTotalLines()).isEqualTo(3);
        assertThat(result.getInfoCount()).isEqualTo(2);
        assertThat(result.getErrorCount()).isEqualTo(1);
    }

    @Test
    void checkpointWithMismatchedSnapshotVersionIsIgnored() {
        AnalysisJobEntity job = new AnalysisJobEntity();
        job.setAnalysisName("Surum Uyusmazligi Testi");
        job.setFileName("dummy.log");
        job.setFileSize(1);
        job.setStatus(JobStatus.FAILED);
        job.setProgress(0);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setCancelRequested(false);
        AnalysisJobEntity saved = analysisJobRepository.save(job);

        AnalysisResultAccumulator baselineAccumulator = new AnalysisResultAccumulator(200, 500);
        analysisCheckpointService.saveCheckpoint(saved.getId(), 5, baselineAccumulator);

        var checkpointEntity = checkpointRepository.findById(saved.getId()).orElseThrow();
        checkpointEntity.setSnapshotVersion(AnalysisCheckpointSnapshot.CURRENT_SNAPSHOT_VERSION + 99);
        checkpointRepository.save(checkpointEntity);

        assertThat(analysisCheckpointService.loadCheckpoint(saved.getId())).isEmpty();
    }
}