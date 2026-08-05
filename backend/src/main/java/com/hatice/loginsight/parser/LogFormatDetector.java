package com.hatice.loginsight.parser;

import com.hatice.loginsight.exception.LogFormatCouldNotBeDetectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@Service
public class LogFormatDetector {

    private final LogParserFactory parserFactory;
    private final int defaultSampleSize;
    private final int confidenceThreshold;
    private final int maxScannedLines;

    public LogFormatDetector(LogParserFactory parserFactory, int defaultSampleSize, int confidenceThreshold) {
        this(parserFactory, defaultSampleSize, confidenceThreshold, 2000);
    }

    @Autowired
    public LogFormatDetector(LogParserFactory parserFactory,
                              @Value("${app.log-format-detection.sample-size}") int defaultSampleSize,
                              @Value("${app.log-format-detection.confidence-threshold}") int confidenceThreshold,
                              @Value("${app.log-format-detection.max-scanned-lines}") int maxScannedLines) {
        this.parserFactory = parserFactory;
        this.defaultSampleSize = defaultSampleSize;
        this.confidenceThreshold = confidenceThreshold;
        this.maxScannedLines = maxScannedLines;
    }

    
    public List<String> collectSampleLines(Path filePath, int maxSamples) throws IOException {
        return collectSampleLines(filePath, maxSamples, UnaryOperator.identity());
    }

    public List<String> collectSampleLines(Path filePath, int maxSamples, UnaryOperator<String> lineTransform)
            throws IOException {
        List<String> samples = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String rawLine;
            int scanned = 0;
            while (samples.size() < maxSamples && scanned < maxScannedLines
                    && (rawLine = reader.readLine()) != null) {
                scanned++;
                String transformed = lineTransform.apply(rawLine);
                if (transformed.isBlank()) {
                    continue;
                }
                if (ContinuationLineDetector.isContinuationLine(transformed)) {
                    continue;
                }
                samples.add(transformed);
            }
        }
        return samples;
    }

    public int getDefaultSampleSize() {
        return defaultSampleSize;
    }

    public LogFormatDetectionResult detect(List<String> sampleLines) {
        if (sampleLines.isEmpty()) {
            throw new LogFormatCouldNotBeDetectedException(
                    "Dosyada format algılaması için anlamlı (boş olmayan) satır bulunamadı");
        }

        LogFormat bestFormat = null;
        int bestMatched = 0;
        int bestConfidence = -1;

        for (LogParser parser : parserFactory.getAllParsers()) {
            if (parser.getFormat() == LogFormat.PLAIN_TEXT) {
                continue;
            }
            int matched = countMatches(parser, sampleLines);
            int confidence = (int) Math.round((matched * 100.0) / sampleLines.size());
            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                bestMatched = matched;
                bestFormat = parser.getFormat();
            }
        }

        if (bestConfidence >= confidenceThreshold) {
            return new LogFormatDetectionResult(bestFormat, bestConfidence, sampleLines.size(), bestMatched);
        }

        return new LogFormatDetectionResult(LogFormat.PLAIN_TEXT, bestConfidence, sampleLines.size(), bestMatched);
    }

    private int countMatches(LogParser parser, List<String> sampleLines) {
        int matched = 0;
        for (String line : sampleLines) {
            if (parser.canParse(line)) {
                matched++;
            }
        }
        return matched;
    }
}