package com.hatice.loginsight.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "log_analysis")
public class LogAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "total_lines", nullable = false)
    private int totalLines;

    @Column(name = "info_count", nullable = false)
    private int infoCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "exception_count", nullable = false)
    private int exceptionCount;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    @Column(name = "processing_duration_ms", nullable = false)
    private long processingDurationMs;

    @Column(name = "analysis_name")
    private String analysisName;

    @Column(name = "requested_parser_type", length = 30)
    private String requestedParserType;

    @Column(name = "detected_log_format", length = 30)
    private String detectedLogFormat;

    @Column(name = "detected_envelope", length = 30)
    private String detectedEnvelope;

    @Column(name = "parsed_entry_count")
    private Integer parsedEntryCount;

    @Column(name = "unparsed_line_count")
    private Integer unparsedLineCount;

    @Column(name = "first_log_timestamp")
    private Instant firstLogTimestamp;

    @Column(name = "last_log_timestamp")
    private Instant lastLogTimestamp;

    @Column(name = "multiline_exception_count")
    private Integer multilineExceptionCount;

    @Column(name = "parse_quality_score")
    private Integer parseQualityScore;

    @Column(name = "format_confidence")
    private Integer formatConfidence;

    @Column(name = "format_detection_sample_size")
    private Integer formatDetectionSampleSize;

    @Column(name = "matched_sample_count")
    private Integer matchedSampleCount;

    @OneToMany(mappedBy = "logAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FrequentErrorEntity> frequentErrors = new ArrayList<>();

    public LogAnalysisEntity() {
    }

    public void addFrequentError(FrequentErrorEntity error) {
        frequentErrors.add(error);
        error.setLogAnalysis(this);
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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
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

    public List<FrequentErrorEntity> getFrequentErrors() {
        return frequentErrors;
    }

    public void setFrequentErrors(List<FrequentErrorEntity> frequentErrors) {
        this.frequentErrors = frequentErrors;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
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
}