package com.hatice.loginsight.dto;

import java.time.Instant;
import java.util.List;

public class AnalysisDetailDto {

    private Long id;
    private String fileName;
    private String analysisName;
    private long fileSize;
    private Instant analyzedAt;
    private long processingDurationMs;
    private long totalLines;
    private long infoCount;
    private long warningCount;
    private long errorCount;
    private long exceptionCount;
    private List<ErrorFrequency> mostFrequentErrors;

    private String requestedParserType;
    private String detectedLogFormat;
    private String detectedEnvelope;
    private Long parsedEntryCount;
    private Long unparsedLineCount;
    private Double unparsedLinePercentage;
    private Instant firstLogTimestamp;
    private Instant lastLogTimestamp;
    private Long multilineExceptionCount;
    private List<LoggerFrequency> mostFrequentLoggers;
    private List<ThreadFrequency> mostFrequentThreads;
    private List<StatusCodeCount> statusCodeDistribution;
    private List<HttpMethodCount> httpMethodDistribution;
    private List<TimelineBucketDto> timeline;
    private Integer parseQualityScore;
    private String timelineGranularity;
    private Integer formatConfidence;
    private Integer formatDetectionSampleSize;
    private Integer matchedSampleCount;
    private AppliedFiltersDto appliedFilters;

    public AnalysisDetailDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Instant analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public long getProcessingDurationMs() {
        return processingDurationMs;
    }

    public void setProcessingDurationMs(long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
    }

    public long getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(long totalLines) {
        this.totalLines = totalLines;
    }

    public long getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(long infoCount) {
        this.infoCount = infoCount;
    }

    public long getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(long warningCount) {
        this.warningCount = warningCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(long exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    public List<ErrorFrequency> getMostFrequentErrors() {
        return mostFrequentErrors;
    }

    public void setMostFrequentErrors(List<ErrorFrequency> mostFrequentErrors) {
        this.mostFrequentErrors = mostFrequentErrors;
    }

    public String getRequestedParserType() {
        return requestedParserType;
    }

    public void setRequestedParserType(String requestedParserType) {
        this.requestedParserType = requestedParserType;
    }

    public String getDetectedLogFormat() {
        return detectedLogFormat;
    }

    public void setDetectedLogFormat(String detectedLogFormat) {
        this.detectedLogFormat = detectedLogFormat;
    }

    public String getDetectedEnvelope() {
        return detectedEnvelope;
    }

    public void setDetectedEnvelope(String detectedEnvelope) {
        this.detectedEnvelope = detectedEnvelope;
    }

    public Long getParsedEntryCount() {
        return parsedEntryCount;
    }

    public void setParsedEntryCount(Long parsedEntryCount) {
        this.parsedEntryCount = parsedEntryCount;
    }

    public Long getUnparsedLineCount() {
        return unparsedLineCount;
    }

    public void setUnparsedLineCount(Long unparsedLineCount) {
        this.unparsedLineCount = unparsedLineCount;
    }

    public Double getUnparsedLinePercentage() {
        return unparsedLinePercentage;
    }

    public void setUnparsedLinePercentage(Double unparsedLinePercentage) {
        this.unparsedLinePercentage = unparsedLinePercentage;
    }

    public Instant getFirstLogTimestamp() {
        return firstLogTimestamp;
    }

    public void setFirstLogTimestamp(Instant firstLogTimestamp) {
        this.firstLogTimestamp = firstLogTimestamp;
    }

    public Instant getLastLogTimestamp() {
        return lastLogTimestamp;
    }

    public void setLastLogTimestamp(Instant lastLogTimestamp) {
        this.lastLogTimestamp = lastLogTimestamp;
    }

    public Long getMultilineExceptionCount() {
        return multilineExceptionCount;
    }

    public void setMultilineExceptionCount(Long multilineExceptionCount) {
        this.multilineExceptionCount = multilineExceptionCount;
    }

    public List<LoggerFrequency> getMostFrequentLoggers() {
        return mostFrequentLoggers;
    }

    public void setMostFrequentLoggers(List<LoggerFrequency> mostFrequentLoggers) {
        this.mostFrequentLoggers = mostFrequentLoggers;
    }

    public List<ThreadFrequency> getMostFrequentThreads() {
        return mostFrequentThreads;
    }

    public void setMostFrequentThreads(List<ThreadFrequency> mostFrequentThreads) {
        this.mostFrequentThreads = mostFrequentThreads;
    }

    public List<StatusCodeCount> getStatusCodeDistribution() {
        return statusCodeDistribution;
    }

    public void setStatusCodeDistribution(List<StatusCodeCount> statusCodeDistribution) {
        this.statusCodeDistribution = statusCodeDistribution;
    }

    public List<HttpMethodCount> getHttpMethodDistribution() {
        return httpMethodDistribution;
    }

    public void setHttpMethodDistribution(List<HttpMethodCount> httpMethodDistribution) {
        this.httpMethodDistribution = httpMethodDistribution;
    }

    public List<TimelineBucketDto> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TimelineBucketDto> timeline) {
        this.timeline = timeline;
    }

    public Integer getParseQualityScore() {
        return parseQualityScore;
    }

    public void setParseQualityScore(Integer parseQualityScore) {
        this.parseQualityScore = parseQualityScore;
    }

    public String getTimelineGranularity() {
        return timelineGranularity;
    }

    public void setTimelineGranularity(String timelineGranularity) {
        this.timelineGranularity = timelineGranularity;
    }

    public Integer getFormatConfidence() {
        return formatConfidence;
    }

    public void setFormatConfidence(Integer formatConfidence) {
        this.formatConfidence = formatConfidence;
    }

    public Integer getFormatDetectionSampleSize() {
        return formatDetectionSampleSize;
    }

    public void setFormatDetectionSampleSize(Integer formatDetectionSampleSize) {
        this.formatDetectionSampleSize = formatDetectionSampleSize;
    }

    public Integer getMatchedSampleCount() {
        return matchedSampleCount;
    }

    public void setMatchedSampleCount(Integer matchedSampleCount) {
        this.matchedSampleCount = matchedSampleCount;
    }

    public AppliedFiltersDto getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(AppliedFiltersDto appliedFilters) {
        this.appliedFilters = appliedFilters;
    }
}