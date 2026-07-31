package com.hatice.loginsight.parser;

public class LogFormatDetectionResult {

    private final LogFormat detectedFormat;
    private final int formatConfidence;
    private final int sampleSize;
    private final int matchedSampleCount;

    public LogFormatDetectionResult(LogFormat detectedFormat, int formatConfidence,
                                     int sampleSize, int matchedSampleCount) {
        this.detectedFormat = detectedFormat;
        this.formatConfidence = formatConfidence;
        this.sampleSize = sampleSize;
        this.matchedSampleCount = matchedSampleCount;
    }

    public LogFormat getDetectedFormat() {
        return detectedFormat;
    }

    public int getFormatConfidence() {
        return formatConfidence;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public int getMatchedSampleCount() {
        return matchedSampleCount;
    }
}