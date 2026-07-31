package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.AnalysisLoggerStatRepository;
import com.hatice.loginsight.repository.AnalysisTimelineStatRepository;
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
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnalysisJobV5PersistenceTest extends AbstractIntegrationTest {

    private static final String SPRING_BOOT_CONTENT =
            "2026-01-01 10:00:00.000 INFO 1 --- [main] com.example.AppService : Application started\n"
                    + "2026-01-01 10:00:01.000 WARN 1 --- [main] com.example.AppService : Deprecated config key used\n"
                    + "2026-01-01 10:00:02.000 ERROR 1 --- [main] com.example.AppService : User 111 not found\n"
                    + "2026-01-01 10:00:03.000 ERROR 1 --- [main] com.example.AppService : User 222 not found\n";

    @Autowired
    private AnalysisJobService analysisJobService;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    @Autowired
    private AnalysisLoggerStatRepository loggerStatRepository;

    @Autowired
    private AnalysisTimelineStatRepository timelineStatRepository;

    @BeforeEach
    void cleanUp() {
        analysisJobRepository.deleteAllInBatch();
        logAnalysisRepository.deleteAllInBatch();
    }

    private MockMultipartFile springBootFile(String name) {
        return new MockMultipartFile("file", name, "text/plain",
                SPRING_BOOT_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private AnalysisJobEntity awaitStatus(UUID jobId, JobStatus expectedStatus) {
        AnalysisJobEntity job = awaitCondition(() -> analysisJobRepository.findById(jobId).orElseThrow(),
                candidate -> candidate.getStatus() == expectedStatus);
        if (job.getStatus() != expectedStatus) {
            throw new AssertionError("Job zaman asimina ugradi, beklenen durum " + expectedStatus
                    + " ama son gorulen durum " + job.getStatus() + " (errorCode=" + job.getErrorCode() + ")");
        }
        return job;
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
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return last;
    }

    @Test
    void persistsV5FieldsForAutoDetectedSpringBootFormat() {
        AnalysisJobEntity created = analysisJobService.createJob(
                springBootFile("auto-detect.log"), "V5 Auto Detect Testi",
                null, null, null, null, null, null, null, null, null, null);

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);
        assertThat(finished.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(finished.getDetectedLogFormat()).isEqualTo("SPRING_BOOT");

        LogAnalysisEntity entity = logAnalysisRepository.findById(finished.getAnalysisId()).orElseThrow();

        assertThat(entity.getDetectedLogFormat()).isEqualTo("SPRING_BOOT");
        assertThat(entity.getRequestedParserType()).isEqualTo("AUTO");
        assertThat(entity.getParsedEntryCount()).isEqualTo(4);
        assertThat(entity.getUnparsedLineCount()).isEqualTo(0);
        assertThat(entity.getFormatConfidence()).isEqualTo(100);
        assertThat(entity.getParseQualityScore()).isEqualTo(100);
        assertThat(entity.getFirstLogTimestamp()).isNotNull();
        assertThat(entity.getLastLogTimestamp()).isNotNull();
    }

    @Test
    void persistsNormalizedFrequentErrorGrouping() {
        AnalysisJobEntity created = analysisJobService.createJob(springBootFile("errors.log"), "Hata Grubu Testi");

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);
        LogAnalysisEntity entity = logAnalysisRepository.findByIdWithFrequentErrors(finished.getAnalysisId()).orElseThrow();

        assertThat(entity.getFrequentErrors()).hasSize(1);
        assertThat(entity.getFrequentErrors().get(0).getNormalizedMessage()).isEqualTo("User <NUMBER> not found");
        assertThat(entity.getFrequentErrors().get(0).getOccurrenceCount()).isEqualTo(2);
        assertThat(entity.getFrequentErrors().get(0).getMessage()).contains("not found");
    }

    @Test
    void persistsLoggerAndTimelineStats() {
        AnalysisJobEntity created = analysisJobService.createJob(springBootFile("stats.log"), "Istatistik Testi");

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);
        Long analysisId = finished.getAnalysisId();

        assertThat(loggerStatRepository.findByLogAnalysisId(analysisId))
                .anyMatch(stat -> stat.getLoggerName().equals("com.example.AppService") && stat.getEntryCount() == 4);

        assertThat(timelineStatRepository.findByLogAnalysisIdOrderByBucketStartAsc(analysisId))
                .isNotEmpty();
    }

    @Test
    void failsJobWhenManuallySelectedParserCannotParseFile() {
        AnalysisJobEntity created = analysisJobService.createJob(
                springBootFile("wrong-parser.log"), "Yanlis Parser Testi",
                "JSON", null, null, null, null, null, null, null, null, null);

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.FAILED);

        assertThat(finished.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(finished.getErrorCode()).isEqualTo("SELECTED_PARSER_CANNOT_PARSE_FILE");
    }

    @Test
    void appliesLevelFilterDuringAnalysis() {
        AnalysisJobEntity created = analysisJobService.createJob(
                springBootFile("filtered.log"), "Filtre Testi",
                null, null, null, "ERROR", null, null, null, null, null, null);

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);
        LogAnalysisEntity entity = logAnalysisRepository.findById(finished.getAnalysisId()).orElseThrow();

        assertThat(entity.getErrorCount()).isEqualTo(2);
        assertThat(entity.getInfoCount()).isEqualTo(0);
        assertThat(entity.getWarningCount()).isEqualTo(0);
    }
}