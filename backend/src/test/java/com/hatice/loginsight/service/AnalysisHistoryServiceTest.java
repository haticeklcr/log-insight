package com.hatice.loginsight.service;

import com.hatice.loginsight.AbstractIntegrationTest;
import com.hatice.loginsight.dto.AnalysisDetailDto;
import com.hatice.loginsight.dto.PagedResponse;
import com.hatice.loginsight.entity.FrequentErrorEntity;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.exception.AnalysisNotFoundException;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AnalysisHistoryServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AnalysisHistoryService analysisHistoryService;

    @Autowired
    private LogAnalysisRepository logAnalysisRepository;

    @BeforeEach
    void cleanUp() {
        logAnalysisRepository.deleteAll();
    }

    private LogAnalysisEntity createAnalysis(String fileName, int errorCount) {
        LogAnalysisEntity entity = new LogAnalysisEntity();
        entity.setFileName(fileName);
        entity.setFileSize(100);
        entity.setTotalLines(10);
        entity.setInfoCount(5);
        entity.setWarningCount(2);
        entity.setErrorCount(errorCount);
        entity.setExceptionCount(1);
        entity.setAnalyzedAt(Instant.now());
        entity.setProcessingDurationMs(50);
        entity.addFrequentError(new FrequentErrorEntity("Sample error", 3));
        return logAnalysisRepository.save(entity);
    }

    @Test
    void listsAnalysesWithPagination() {
        createAnalysis("first.log", 2);
        createAnalysis("second.log", 4);

        PagedResponse<?> response = analysisHistoryService.listAnalyses(0, 10, "analyzedAt", "desc", null, null);

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void filtersByFileName() {
        createAnalysis("production.log", 2);
        createAnalysis("staging.log", 3);

        PagedResponse<?> response = analysisHistoryService.listAnalyses(0, 10, "analyzedAt", "desc", "prod", null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void filtersByMinimumErrorCount() {
        createAnalysis("low-error.log", 1);
        createAnalysis("high-error.log", 10);

        PagedResponse<?> response = analysisHistoryService.listAnalyses(0, 10, "analyzedAt", "desc", null, 5);

        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void returnsAnalysisDetailById() {
        LogAnalysisEntity saved = createAnalysis("detail.log", 2);

        AnalysisDetailDto detail = analysisHistoryService.getAnalysisDetail(saved.getId());

        assertThat(detail.getFileName()).isEqualTo("detail.log");
        assertThat(detail.getMostFrequentErrors()).hasSize(1);
        assertThat(detail.getMostFrequentErrors().get(0).getMessage()).isEqualTo("Sample error");
    }

    @Test
    void throwsNotFoundForMissingAnalysisId() {
        assertThrows(AnalysisNotFoundException.class, () -> analysisHistoryService.getAnalysisDetail(999999L));
    }

    @Test
    void deletesAnalysisAndCascadesFrequentErrors() {
        LogAnalysisEntity saved = createAnalysis("to-delete.log", 2);
        Long id = saved.getId();

        analysisHistoryService.deleteAnalysis(id);

        assertThat(logAnalysisRepository.findById(id)).isEmpty();
    }

    @Test
    void throwsNotFoundWhenDeletingMissingAnalysis() {
        assertThrows(AnalysisNotFoundException.class, () -> analysisHistoryService.deleteAnalysis(999999L));
    }
}