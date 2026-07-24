package com.hatice.loginsight.service;

import com.hatice.loginsight.dto.LogAnalysisResult;
import com.hatice.loginsight.exception.EmptyFileException;
import com.hatice.loginsight.exception.FileTooLargeException;
import com.hatice.loginsight.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.repository.LogAnalysisRepository;

class LogAnalysisServiceTest {

    private final LogAnalysisRepository logAnalysisRepository = mock(LogAnalysisRepository.class);
    private final LogAnalysisService service = new LogAnalysisService(DataSize.ofMegabytes(1), logAnalysisRepository);

    {
        when(logAnalysisRepository.save(any(LogAnalysisEntity.class))).thenAnswer(invocation -> {
            LogAnalysisEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });
    }
    private static final String SAMPLE_LOG_CONTENT =
            "2026-01-01 10:00:00 INFO Application started\n" +
            "2026-01-01 10:00:01 INFO Loading configuration\n" +
            "2026-01-01 10:00:02 WARN Deprecated config key used\n" +
            "2026-01-01 10:00:03 ERROR: Connection refused\n" +
            "2026-01-01 10:00:04 ERROR: Connection refused\n" +
            "2026-01-01 10:00:05 ERROR: Connection refused\n" +
            "2026-01-01 10:00:06 ERROR: Request timeout\n" +
            "2026-01-01 10:00:07 WARN Deprecated API call triggered a NullPointerException\n" +
            "2026-01-01 10:00:08 INFO Shutting down\n";

    @Test
    void analyzesValidLogFileSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "application.log", "text/plain",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));

        LogAnalysisResult result = service.analyze(file);

        assertThat(result.getFileName()).isEqualTo("application.log");
        assertThat(result.getTotalLines()).isEqualTo(9);
    }

    @Test
    void countsLogLevelsCorrectly() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "application.log", "text/plain",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));

        LogAnalysisResult result = service.analyze(file);

        assertThat(result.getInfoCount()).isEqualTo(3);
        assertThat(result.getWarningCount()).isEqualTo(2);
        assertThat(result.getErrorCount()).isEqualTo(4);
        assertThat(result.getExceptionCount()).isEqualTo(1);
    }

    @Test
    void groupsRepeatedErrorMessagesCorrectly() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "application.log", "text/plain",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));

        LogAnalysisResult result = service.analyze(file);

        assertThat(result.getMostFrequentErrors()).hasSize(2);
        assertThat(result.getMostFrequentErrors().get(0).getMessage()).isEqualTo("Connection refused");
        assertThat(result.getMostFrequentErrors().get(0).getCount()).isEqualTo(3);
        assertThat(result.getMostFrequentErrors().get(1).getMessage()).isEqualTo("Request timeout");
        assertThat(result.getMostFrequentErrors().get(1).getCount()).isEqualTo(1);
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.log", "text/plain", new byte[0]);

        assertThrows(EmptyFileException.class, () -> service.analyze(file));
    }

    @Test
    void rejectsUnsupportedFileExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "application.pdf", "application/pdf",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));

        assertThrows(UnsupportedFileTypeException.class, () -> service.analyze(file));
    }

    @Test
    void rejectsFileExceedingConfiguredSizeLimit() {
        byte[] oversized = new byte[2 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.log", "text/plain", oversized);

        assertThrows(FileTooLargeException.class, () -> service.analyze(file));
    }

    @Test
    void appliesDifferentConfiguredSizeLimitWhenInjectedDifferently() {
        LogAnalysisService serviceWithSmallerLimit =
                new LogAnalysisService(DataSize.ofKilobytes(1), logAnalysisRepository);
        byte[] slightlyOverOneKb = new byte[2 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.log", "text/plain", slightlyOverOneKb);

        assertThrows(FileTooLargeException.class, () -> serviceWithSmallerLimit.analyze(file));
    }
}