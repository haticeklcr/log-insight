package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.AnalysisTimelineStatRepository;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnalysisJobV6EnvelopeTest extends AbstractIntegrationTest {

    @Autowired
    private AnalysisJobService analysisJobService;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    @Autowired
    private AnalysisTimelineStatRepository timelineStatRepository;

    @BeforeEach
    void cleanUp() {
        analysisJobRepository.deleteAllInBatch();
        logAnalysisRepository.deleteAllInBatch();
    }

    private MockMultipartFile fixtureFile(String resourceName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("fixtures/" + resourceName)) {
            return new MockMultipartFile("file", resourceName, "text/plain", is.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private MockMultipartFile textFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    private AnalysisJobEntity awaitStatus(UUID jobId, JobStatus expectedStatus) {
        return awaitCondition(() -> analysisJobRepository.findById(jobId).orElseThrow(),
                job -> job.getStatus() == expectedStatus);
    }

    private <T> T awaitCondition(Supplier<T> supplier, Predicate<T> condition) {
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
        throw new AssertionError("Zaman asimi - son durum: " + last);
    }

    @Test
    void journaldPrefixedFileIsParsedCorrectly() {
        AnalysisJobEntity created = analysisJobService.createJob(
                fixtureFile("envelope-rfc3164-sample.log"), "Journald Testi");

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);
        assertThat(finished.getDetectedEnvelope()).isEqualTo("SYSLOG_RFC3164");
        assertThat(finished.getDetectedLogFormat()).isEqualTo("SPRING_BOOT");

        LogAnalysisEntity result = logAnalysisRepository.findById(finished.getAnalysisId()).orElseThrow();
        assertThat(result.getTotalLines()).isEqualTo(5);
        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getExceptionCount()).isEqualTo(1);
    }

    @Test
    void criPrefixedFileIsParsedCorrectlyAndPartialsAreMerged() {
        AnalysisJobEntity created = analysisJobService.createJob(
                fixtureFile("envelope-cri-sample.log"), "CRI Testi");

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);
        assertThat(finished.getDetectedEnvelope()).isEqualTo("CONTAINER_CRI");

        LogAnalysisEntity result = logAnalysisRepository.findById(finished.getAnalysisId()).orElseThrow();
        assertThat(result.getInfoCount()).isEqualTo(1);
        assertThat(result.getWarningCount()).isEqualTo(1);
        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getExceptionCount()).isEqualTo(1);
    }

    @Test
    void outerTimestampIsUsedAsFallbackWhenInnerTimestampMissing() {
        String content =
                "Jul 30 06:55:07 dc05 app[1]: ERROR odeme basarisiz\n" +
                "Jul 30 06:55:08 dc05 app[1]: INFO devam ediliyor\n";
        AnalysisJobEntity created = analysisJobService.createJob(
                textFile("no-inner-timestamp.log", content), "Fallback Zaman Damgasi Testi");

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);
        LogAnalysisEntity result = logAnalysisRepository.findById(finished.getAnalysisId()).orElseThrow();

        assertThat(result.getFirstLogTimestamp()).isNotNull();
        assertThat(result.getLastLogTimestamp()).isNotNull();
    }

    @Test
    void timelineIsCreatedForEnvelopedFile() {
        AnalysisJobEntity created = analysisJobService.createJob(
                fixtureFile("envelope-rfc3164-sample.log"), "Envelope Timeline Testi");

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);

        assertThat(timelineStatRepository.findByLogAnalysisIdOrderByBucketStartAsc(finished.getAnalysisId()))
                .isNotEmpty();
    }

    @Test
    void unrelatedUnparsableLinesAreCountedAsUnparsed() {
        String content =
                "2026-01-01 10:00:00.000 INFO 1 --- [main] c.e.Foo : normal satir bir\n" +
                "2026-01-01 10:00:01.000 INFO 1 --- [main] c.e.Foo : normal satir iki\n" +
                "2026-01-01 10:00:02.000 INFO 1 --- [main] c.e.Foo : normal satir uc\n" +
                "2026-01-01 10:00:03.000 INFO 1 --- [main] c.e.Foo : normal satir dort\n" +
                "bu satir hicbir sekilde parse edilemez ve stack trace da degil\n" +
                "baska boyle bir satir daha\n";
        AnalysisJobEntity created = analysisJobService.createJob(
                textFile("garbage-lines.log", content), "Parse Edilemeyen Satir Testi");

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);
        assertThat(finished.getDetectedLogFormat()).isEqualTo("SPRING_BOOT");
        LogAnalysisEntity result = logAnalysisRepository.findById(finished.getAnalysisId()).orElseThrow();

        assertThat(result.getTotalLines()).isEqualTo(6);
        assertThat(result.getUnparsedLineCount()).isEqualTo(2L);
    }

    @Test
    void multilineExceptionInEnvelopedFileIsCountedAsSingleException() {
        String content =
                "Jul 30 06:55:07 dc05 app[1]: 2026-01-01 10:00:00.000 ERROR 1 --- [main] c.e.Foo : "
                        + "Odeme basarisiz: java.lang.IllegalStateException: pool bos\n" +
                "Jul 30 06:55:07 dc05 app[1]: \tat com.example.A.a(A.java:1)\n" +
                "Jul 30 06:55:07 dc05 app[1]: \tat com.example.B.b(B.java:2)\n" +
                "Jul 30 06:55:07 dc05 app[1]: \tat com.example.C.c(C.java:3)\n";
        AnalysisJobEntity created = analysisJobService.createJob(
                textFile("multiline-enveloped.log", content), "Onekli Multiline Testi");

        AnalysisJobEntity finished = awaitStatus(created.getId(), JobStatus.SUCCEEDED);
        LogAnalysisEntity result = logAnalysisRepository.findById(finished.getAnalysisId()).orElseThrow();

        assertThat(result.getTotalLines()).isEqualTo(4);
        assertThat(result.getExceptionCount()).isEqualTo(1);
        assertThat(result.getUnparsedLineCount()).isEqualTo(0);
    }
}