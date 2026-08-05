package com.hatice.loginsight.parser;

public class LogEnvelopeDetectionResult {

    private final LogEnvelope detectedEnvelope;
    private final int confidence;
    private final int sampleSize;
    private final int matchedSampleCount;

    public LogEnvelopeDetectionResult(LogEnvelope detectedEnvelope, int confidence,
                                       int sampleSize, int matchedSampleCount) {
        this.detectedEnvelope = detectedEnvelope;
        this.confidence = confidence;
        this.sampleSize = sampleSize;
        this.matchedSampleCount = matchedSampleCount;
    }

    public LogEnvelope getDetectedEnvelope() {
        return detectedEnvelope;
    }

    public int getConfidence() {
        return confidence;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public int getMatchedSampleCount() {
        return matchedSampleCount;
    }
}