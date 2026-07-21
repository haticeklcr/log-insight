package com.hatice.loginsight.service;

import com.hatice.loginsight.dto.ErrorFrequency;
import com.hatice.loginsight.dto.LogAnalysisResult;
import com.hatice.loginsight.exception.EmptyFileException;
import com.hatice.loginsight.exception.FileTooLargeException;
import com.hatice.loginsight.exception.UnsupportedFileTypeException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LogAnalysisService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".log", ".txt");

    public LogAnalysisResult analyze(MultipartFile file) {
        validate(file);

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

        LogAnalysisResult result = new LogAnalysisResult();
        result.setFileName(file.getOriginalFilename());
        result.setTotalLines(totalLines);
        result.setInfoCount(infoCount);
        result.setWarningCount(warningCount);
        result.setErrorCount(errorCount);
        result.setExceptionCount(exceptionCount);
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

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileTooLargeException("Dosya boyutu izin verilen maksimum sınırı (5MB) aşıyor");
        }
    }
}