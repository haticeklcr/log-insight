package com.hatice.loginsight.service;

import com.hatice.loginsight.dto.ErrorFrequency;
import com.hatice.loginsight.dto.LogAnalysisResult;
import com.hatice.loginsight.entity.FrequentErrorEntity;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.exception.EmptyFileException;
import com.hatice.loginsight.exception.FileTooLargeException;
import com.hatice.loginsight.exception.UnsupportedFileTypeException;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LogAnalysisService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".log", ".txt");

    private final DataSize maxFileSize;
    private final LogAnalysisRepository logAnalysisRepository;

    public LogAnalysisService(@Value("${app.log-analysis.max-file-size:10MB}") DataSize maxFileSize,
                               LogAnalysisRepository logAnalysisRepository) {
        this.maxFileSize = maxFileSize;
        this.logAnalysisRepository = logAnalysisRepository;
    }

    @Transactional
    public LogAnalysisResult analyze(MultipartFile file) {
        validate(file);

        long startTime = System.currentTimeMillis();

        int totalLines = 0;
        int infoCount = 0;
        int warningCount = 0;
        int errorCount = 0;
        int exceptionCount = 0;
        Map<String, Integer> errorMessageCounts = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

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
            }
        } catch (IOException e) {
            throw new RuntimeException("Log dosyası okunurken bir hata oluştu", e);
        }

        List<ErrorFrequency> mostFrequentErrors = errorMessageCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .map(entry -> new ErrorFrequency(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        long processingDurationMs = System.currentTimeMillis() - startTime;

        LogAnalysisEntity entity = new LogAnalysisEntity();
        entity.setFileName(file.getOriginalFilename());
        entity.setFileSize(file.getSize());
        entity.setTotalLines(totalLines);
        entity.setInfoCount(infoCount);
        entity.setWarningCount(warningCount);
        entity.setErrorCount(errorCount);
        entity.setExceptionCount(exceptionCount);
        entity.setAnalyzedAt(Instant.now());
        entity.setProcessingDurationMs(processingDurationMs);

        for (ErrorFrequency errorFrequency : mostFrequentErrors) {
            entity.addFrequentError(new FrequentErrorEntity(errorFrequency.getMessage(), errorFrequency.getCount()));
        }

        LogAnalysisEntity savedEntity = logAnalysisRepository.save(entity);

        LogAnalysisResult result = new LogAnalysisResult();
        result.setId(savedEntity.getId());
        result.setFileName(savedEntity.getFileName());
        result.setTotalLines(savedEntity.getTotalLines());
        result.setInfoCount(savedEntity.getInfoCount());
        result.setWarningCount(savedEntity.getWarningCount());
        result.setErrorCount(savedEntity.getErrorCount());
        result.setExceptionCount(savedEntity.getExceptionCount());
        result.setMostFrequentErrors(mostFrequentErrors);
        return result;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException("Yüklenen dosya boş");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || ALLOWED_EXTENSIONS.stream().noneMatch(ext -> filename.toLowerCase().endsWith(ext))) {
            throw new UnsupportedFileTypeException("Sadece .log ve .txt uzantılı dosyalar desteklenir");
        }

        if (file.getSize() > maxFileSize.toBytes()) {
            throw new FileTooLargeException(
                    "Dosya boyutu izin verilen maksimum sınırı (" + maxFileSize.toMegabytes() + "MB) aşıyor");
        }
    }
}