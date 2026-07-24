package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.dto.LogAnalysisResult;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class LogAnalysisPersistenceTest extends AbstractIntegrationTest {

    @Autowired
    private LogAnalysisService logAnalysisService;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final String SAMPLE_LOG_CONTENT =
            "2026-01-01 10:00:00 INFO Application started\n" +
            "2026-01-01 10:00:03 ERROR: Connection refused\n" +
            "2026-01-01 10:00:04 ERROR: Connection refused\n";

    @Test
    void savesAnalysisResultToDatabase() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "persisted.log", "text/plain",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));

        LogAnalysisResult result = logAnalysisService.analyze(file);

        assertThat(result.getId()).isNotNull();

        Optional<LogAnalysisEntity> savedEntity = logAnalysisRepository.findById(result.getId());
        assertThat(savedEntity).isPresent();
        assertThat(savedEntity.get().getFileName()).isEqualTo("persisted.log");
        assertThat(savedEntity.get().getTotalLines()).isEqualTo(3);
    }

    @Test
    @Transactional
    void linksFrequentErrorsToAnalysisRecord() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "with-errors.log", "text/plain",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));

        LogAnalysisResult result = logAnalysisService.analyze(file);

        LogAnalysisEntity savedEntity = logAnalysisRepository.findById(result.getId()).orElseThrow();

        assertThat(savedEntity.getFrequentErrors()).hasSize(1);
        assertThat(savedEntity.getFrequentErrors().get(0).getMessage()).isEqualTo("Connection refused");
        assertThat(savedEntity.getFrequentErrors().get(0).getOccurrenceCount()).isEqualTo(2);
        assertThat(savedEntity.getFrequentErrors().get(0).getLogAnalysis().getId()).isEqualTo(savedEntity.getId());
    }

    @Test
    void rollsBackTransactionWhenSaveFails() {
        long countBefore = logAnalysisRepository.count();

        MockMultipartFile file = new MockMultipartFile(
                "file", "rollback-test.log", "text/plain",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () ->
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        logAnalysisService.analyze(file);
                        throw new RuntimeException("Simulated failure after save");
                    }
                }));

        long countAfter = logAnalysisRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }
}