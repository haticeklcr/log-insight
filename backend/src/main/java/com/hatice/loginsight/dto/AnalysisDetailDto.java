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
    private int totalLines;
    private int infoCount;
    private int warningCount;
    private int errorCount;
    private int exceptionCount;
    private List<ErrorFrequency> mostFrequentErrors;

    private String requestedParserType;
    private String detectedLogFormat;
    private Integer parsedEntryCount;
    private Integer unparsedLineCount;
    private Double unparsedLinePercentage;
    private Instant firstLogTimestamp;
    private Instant lastLogTimestamp;
    private Integer multilineExceptionCount;
    private List<LoggerFrequency> mostFrequentLoggers;
    private List<ThreadFrequency> mostFrequentThreads;
    private List<StatusCodeCount> statusCodeDistribution;
    private List<HttpMethodCount> httpMethodDistribution;
    private List<TimelineBucketDto> timeline;
    private Integer parseQualityScore;
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

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(int infoCount) {
        this.infoCount = infoCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(int exceptionCount) {
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

    public Integer getParsedEntryCount() {
        return parsedEntryCount;
    }

    public void setParsedEntryCount(Integer parsedEntryCount) {
        this.parsedEntryCount = parsedEntryCount;
    }

    public Integer getUnparsedLineCount() {
        return unparsedLineCount;
    }

    public void setUnparsedLineCount(Integer unparsedLineCount) {
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

    public Integer getMultilineExceptionCount() {
        return multilineExceptionCount;
    }

    public void setMultilineExceptionCount(Integer multilineExceptionCount) {
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