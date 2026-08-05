package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisHttpMethodStatEntity;
import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.AnalysisLoggerStatEntity;
import com.hatice.loginsight.entity.AnalysisStatusCodeStatEntity;
import com.hatice.loginsight.entity.AnalysisThreadStatEntity;
import com.hatice.loginsight.entity.FrequentErrorEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.exception.LogFormatCouldNotBeDetectedException;
import com.hatice.loginsight.exception.UnsupportedFilterForParserException;
import com.hatice.loginsight.parser.AssembledCriRecord;
import com.hatice.loginsight.parser.CriPartialRecordAssembler;
import com.hatice.loginsight.parser.EnvelopeDetector;
import com.hatice.loginsight.parser.EnvelopeStripResult;
import com.hatice.loginsight.parser.ExceptionInfoExtractor;
import com.hatice.loginsight.parser.LogEnvelope;
import com.hatice.loginsight.parser.LogEnvelopeDetectionResult;
import com.hatice.loginsight.parser.LogFormat;
import com.hatice.loginsight.parser.LogFormatDetectionResult;
import com.hatice.loginsight.parser.LogFormatDetector;
import com.hatice.loginsight.parser.LogMessageNormalizer;
import com.hatice.loginsight.parser.LogParser;
import com.hatice.loginsight.parser.LogParserFactory;
import com.hatice.loginsight.parser.LogRecordGroup;
import com.hatice.loginsight.parser.MultilineExceptionAggregator;
import com.hatice.loginsight.parser.MultilineExceptionInfo;
import com.hatice.loginsight.parser.ParsedLogEntry;
import com.hatice.loginsight.parser.SensitiveDataMasker;
import com.hatice.loginsight.repository.AnalysisHttpMethodStatRepository;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.AnalysisLoggerStatRepository;
import com.hatice.loginsight.repository.AnalysisStatusCodeStatRepository;
import com.hatice.loginsight.repository.AnalysisThreadStatRepository;
import com.hatice.loginsight.repository.AnalysisTimelineStatRepository;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnalysisJobRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalysisJobRunner.class);

    private final AnalysisJobRepository analysisJobRepository;
    private final LogAnalysisRepository logAnalysisRepository;
    private final TempFileStorageService tempFileStorageService;
    private final LogParserFactory parserFactory;
    private final LogFormatDetector formatDetector;
    private final EnvelopeDetector envelopeDetector;
    private final SensitiveDataMasker sensitiveDataMasker;
    private final LogMessageNormalizer logMessageNormalizer;
    private final ExceptionInfoExtractor exceptionInfoExtractor;
    private final AnalysisLoggerStatRepository loggerStatRepository;
    private final AnalysisThreadStatRepository threadStatRepository;
    private final AnalysisStatusCodeStatRepository statusCodeStatRepository;
    private final AnalysisHttpMethodStatRepository httpMethodStatRepository;
    private final AnalysisTimelineStatRepository timelineStatRepository;

    private final int progressInterval;
    private final int maxStackTraceLines;
    private final int maxLogLineLength;
    private final int maxDistinctLoggers;
    private final int maxDistinctErrorGroups;
    private final int maxTimelineBuckets;
    private final int maxUnparsedLinePercentage;
    private final int formatConfidenceThreshold;

    public AnalysisJobRunner(AnalysisJobRepository analysisJobRepository,
                              LogAnalysisRepository logAnalysisRepository,
                              TempFileStorageService tempFileStorageService,
                              LogParserFactory parserFactory,
                              LogFormatDetector formatDetector,
                              EnvelopeDetector envelopeDetector,
                              SensitiveDataMasker sensitiveDataMasker,
                              LogMessageNormalizer logMessageNormalizer,
                              ExceptionInfoExtractor exceptionInfoExtractor,
                              AnalysisLoggerStatRepository loggerStatRepository,
                              AnalysisThreadStatRepository threadStatRepository,
                              AnalysisStatusCodeStatRepository statusCodeStatRepository,
                              AnalysisHttpMethodStatRepository httpMethodStatRepository,
                              AnalysisTimelineStatRepository timelineStatRepository,
                              @Value("${app.analysis-job.progress-interval}") int progressInterval,
                              @Value("${app.log-parsing.max-stack-trace-lines}") int maxStackTraceLines,
                              @Value("${app.log-parsing.max-log-line-length}") int maxLogLineLength,
                              @Value("${app.log-parsing.max-distinct-loggers}") int maxDistinctLoggers,
                              @Value("${app.log-parsing.max-distinct-error-groups}") int maxDistinctErrorGroups,
                              @Value("${app.log-parsing.max-timeline-buckets}") int maxTimelineBuckets,
                              @Value("${app.log-parsing.max-unparsed-line-percentage}") int maxUnparsedLinePercentage,
                              @Value("${app.log-format-detection.confidence-threshold}") int formatConfidenceThreshold) {
        this.analysisJobRepository = analysisJobRepository;
        this.logAnalysisRepository = logAnalysisRepository;
        this.tempFileStorageService = tempFileStorageService;
        this.parserFactory = parserFactory;
        this.formatDetector = formatDetector;
        this.envelopeDetector = envelopeDetector;
        this.sensitiveDataMasker = sensitiveDataMasker;
        this.logMessageNormalizer = logMessageNormalizer;
        this.exceptionInfoExtractor = exceptionInfoExtractor;
        this.loggerStatRepository = loggerStatRepository;
        this.threadStatRepository = threadStatRepository;
        this.statusCodeStatRepository = statusCodeStatRepository;
        this.httpMethodStatRepository = httpMethodStatRepository;
        this.timelineStatRepository = timelineStatRepository;
        this.progressInterval = progressInterval;
        this.maxStackTraceLines = maxStackTraceLines;
        this.maxLogLineLength = maxLogLineLength;
        this.maxDistinctLoggers = maxDistinctLoggers;
        this.maxDistinctErrorGroups = maxDistinctErrorGroups;
        this.maxTimelineBuckets = maxTimelineBuckets;
        this.maxUnparsedLinePercentage = maxUnparsedLinePercentage;
        this.formatConfidenceThreshold = formatConfidenceThreshold;
    }

    @Async("analysisTaskExecutor")
    public void runAnalysis(UUID jobId) {
        Optional<AnalysisJobEntity> maybeJob = analysisJobRepository.findById(jobId);
        if (maybeJob.isEmpty()) {
            return;
        }
        AnalysisJobEntity job = maybeJob.get();

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job = analysisJobRepository.save(job);

        Path filePath = tempFileStorageService.resolve(jobId);

        try {
            List<String> envelopeSampleLines = formatDetector.collectSampleLines(filePath, formatDetector.getDefaultSampleSize());

            LogEnvelopeDetectionResult envelopeDetectionResult = envelopeDetector.detect(envelopeSampleLines);
            LogEnvelope detectedEnvelope = envelopeDetectionResult.getDetectedEnvelope();
            List<String> formatDetectionSampleLines = formatDetector.collectSampleLines(
                    filePath, formatDetector.getDefaultSampleSize(),
                    rawLine -> applyEnvelopeStripping(rawLine, detectedEnvelope));

            LogFormat requestedFormat = parseRequestedFormat(job.getRequestedParserType());
            LogFormat selectedFormat;
            Integer formatConfidence = null;
            Integer sampleSize = null;
            Integer matchedSampleCount = null;

            if (requestedFormat == LogFormat.AUTO) {
                LogFormatDetectionResult detectionResult;
                try {
                    detectionResult = formatDetector.detect(formatDetectionSampleLines);
                } catch (LogFormatCouldNotBeDetectedException e) {
                    handleFailure(job, "LOG_FORMAT_COULD_NOT_BE_DETECTED", e.getMessage());
                    return;
                }
                selectedFormat = detectionResult.getDetectedFormat();
                formatConfidence = detectionResult.getFormatConfidence();
                sampleSize = detectionResult.getSampleSize();
                matchedSampleCount = detectionResult.getMatchedSampleCount();
            } else {
                selectedFormat = requestedFormat;
                if (!formatDetectionSampleLines.isEmpty()) {
                    LogParser candidateParser = parserFactory.getParser(selectedFormat);
                    int matched = 0;
                    for (String sampleLine : formatDetectionSampleLines) {
                        if (candidateParser.canParse(sampleLine)) {
                            matched++;
                        }
                    }
                    int confidence = (int) Math.round((matched * 100.0) / formatDetectionSampleLines.size());
                    if (confidence < formatConfidenceThreshold) {
                        handleFailure(job, "SELECTED_PARSER_CANNOT_PARSE_FILE",
                                "Secilen parser (" + selectedFormat + ") dosya icerigiyle uyumlu degil, guven skoru: " + confidence);
                        return;
                    }
                    formatConfidence = confidence;
                    sampleSize = formatDetectionSampleLines.size();
                    matchedSampleCount = matched;
                }
            }

            LogParser selectedParser = parserFactory.getParser(selectedFormat);

            try {
                AnalysisFilterSupport.validate(
                        selectedFormat,
                        isSet(job.getFilterLogger()),
                        isSet(job.getFilterThread()),
                        isSet(job.getFilterStatusCodes()),
                        isSet(job.getFilterHttpMethods()),
                        isSet(job.getFilterPathContains()));
            } catch (UnsupportedFilterForParserException e) {
                handleFailure(job, "UNSUPPORTED_FILTER_FOR_PARSER", e.getMessage());
                return;
            }

            job.setRequestedParserType(requestedFormat.name());
            job.setDetectedLogFormat(selectedFormat.name());
            job.setDetectedEnvelope(detectedEnvelope.name());
            job = analysisJobRepository.save(job);

            JobFilterCriteria filterCriteria = JobFilterCriteria.from(job);

            long totalBytes = Files.size(filePath);
            AnalysisResultAccumulator accumulator = new AnalysisResultAccumulator(maxDistinctLoggers, maxDistinctErrorGroups);
            LogTimelineAggregator timelineAggregator = new LogTimelineAggregator(maxTimelineBuckets);
            MultilineExceptionAggregator aggregator = new MultilineExceptionAggregator(selectedParser, maxStackTraceLines);
            CriPartialRecordAssembler criAssembler = detectedEnvelope == LogEnvelope.CONTAINER_CRI
                    ? new CriPartialRecordAssembler(maxLogLineLength) : null;

            try (CountingInputStream countingStream = new CountingInputStream(Files.newInputStream(filePath));
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(countingStream, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    accumulator.incrementTotalLines();

                    EnvelopeStripResult stripResult = detectedEnvelope == LogEnvelope.NONE
                            ? null : envelopeDetector.strip(line, detectedEnvelope);

                    consumeLine(line, stripResult, criAssembler, aggregator, selectedParser,
                            accumulator, timelineAggregator, filterCriteria);

                    if (accumulator.getTotalLines() % progressInterval == 0) {
                        boolean checkpointSaved = false;
                        for (int attempt = 1; attempt <= 5 && !checkpointSaved; attempt++) {
                            try {
                                job = analysisJobRepository.findById(jobId).orElseThrow();
                                if (job.isCancelRequested()) {
                                    handleCancellation(job);
                                    return;
                                }
                                int progress = totalBytes == 0 ? 100
                                        : (int) Math.min(99, (countingStream.getBytesRead() * 100) / totalBytes);
                                job.setProgress(progress);
                                job = analysisJobRepository.save(job);
                                checkpointSaved = true;
                            } catch (ObjectOptimisticLockingFailureException e) {
                                if (attempt == 5) {
                                    throw e;
                                }
                            }
                        }
                    }
                }

                if (criAssembler != null) {
                    for (AssembledCriRecord record : criAssembler.flush()) {
                        consumeAssembledCriRecord(record, aggregator, selectedParser, accumulator, timelineAggregator, filterCriteria);
                    }
                }
                aggregator.flush().ifPresent(group -> processGroup(group, selectedParser, accumulator, timelineAggregator, filterCriteria));
            }

            int totalRecords = accumulator.getParsedEntryCount() + accumulator.getUnparsedLineCount();
            if (totalRecords > 0) {
                double unparsedPercentage = (accumulator.getUnparsedLineCount() * 100.0) / totalRecords;
                if (unparsedPercentage > maxUnparsedLinePercentage) {
                    handleFailure(job, "TOO_MANY_UNPARSED_LINES",
                            "Parse edilemeyen satır oranı çok yüksek: %" + Math.round(unparsedPercentage));
                    return;
                }
            }

            handleSuccess(job, accumulator, timelineAggregator, selectedFormat, detectedEnvelope, formatConfidence, sampleSize, matchedSampleCount);

        } catch (IOException e) {
            handleFailure(job, "ANALYSIS_IO_ERROR", "Log dosyasi okunurken bir hata olustu: " + e.getMessage());
        } catch (Exception e) {
            handleFailure(job, "ANALYSIS_UNEXPECTED_ERROR", e.getMessage());
        }
    }

    private LogFormat parseRequestedFormat(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return LogFormat.AUTO;
        }
        return LogFormat.valueOf(rawValue);
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private String applyEnvelopeStripping(String rawLine, LogEnvelope detectedEnvelope) {
        if (detectedEnvelope == LogEnvelope.NONE) {
            return rawLine;
        }
        EnvelopeStripResult stripResult = envelopeDetector.strip(rawLine, detectedEnvelope);
        return stripResult != null ? stripResult.getPayload() : rawLine;
    }

    private void consumeLine(String rawLine, EnvelopeStripResult stripResult, CriPartialRecordAssembler criAssembler,
                              MultilineExceptionAggregator aggregator, LogParser selectedParser,
                              AnalysisResultAccumulator accumulator, LogTimelineAggregator timelineAggregator,
                              JobFilterCriteria filterCriteria) {
        if (criAssembler != null) {
            String stream = stripResult != null ? stripResult.getCriStream() : null;
            String partialTag = stripResult != null ? stripResult.getCriPartialTag() : null;
            String payload = stripResult != null ? stripResult.getPayload() : rawLine;
            Instant fragmentTimestamp = stripResult != null ? stripResult.getEnvelopeTimestamp() : null;

            for (AssembledCriRecord record : criAssembler.offer(stream, partialTag, payload, fragmentTimestamp)) {
                consumeAssembledCriRecord(record, aggregator, selectedParser, accumulator, timelineAggregator, filterCriteria);
            }
            return;
        }

        String logicalLine = stripResult != null ? stripResult.getPayload() : rawLine;
        Instant envelopeTimestamp = stripResult != null ? stripResult.getEnvelopeTimestamp() : null;
        String boundedLine = logicalLine.length() > maxLogLineLength
                ? logicalLine.substring(0, maxLogLineLength) : logicalLine;

        aggregator.offer(boundedLine, envelopeTimestamp)
                .ifPresent(group -> processGroup(group, selectedParser, accumulator, timelineAggregator, filterCriteria));
    }

    private void consumeAssembledCriRecord(AssembledCriRecord record, MultilineExceptionAggregator aggregator,
                                            LogParser selectedParser, AnalysisResultAccumulator accumulator,
                                            LogTimelineAggregator timelineAggregator, JobFilterCriteria filterCriteria) {
        if (record.isIncomplete()) {
            accumulator.recordUnparsedLine();
            return;
        }
        aggregator.offer(record.getPayload(), record.getTimestamp())
                .ifPresent(group -> processGroup(group, selectedParser, accumulator, timelineAggregator, filterCriteria));
    }

    private void processGroup(LogRecordGroup group, LogParser parser, AnalysisResultAccumulator accumulator,
                               LogTimelineAggregator timelineAggregator, JobFilterCriteria filterCriteria) {
        ParsedLogEntry entry = parser.parse(group.getHeaderLine());
        if (entry == null) {
            accumulator.recordUnparsedLine();
            return;
        }

        if (entry.getTimestamp() == null && group.getEnvelopeTimestamp() != null) {
            entry.setTimestamp(group.getEnvelopeTimestamp());
        }

        if (!filterCriteria.matches(entry)) {
            return;
        }

        String maskedMessage = sensitiveDataMasker.mask(entry.getMessage());
        entry.setMessage(maskedMessage);
        entry.setNormalizedMessage(logMessageNormalizer.normalize(maskedMessage));

        MultilineExceptionInfo exceptionInfo = exceptionInfoExtractor.extract(group);
        accumulator.recordEntry(entry, exceptionInfo);
        timelineAggregator.record(entry.getTimestamp(), entry.getLevel(), exceptionInfo != null);
    }

    private void handleSuccess(AnalysisJobEntity job, AnalysisResultAccumulator accumulator,
                                LogTimelineAggregator timelineAggregator, LogFormat detectedFormat,
                                LogEnvelope detectedEnvelope, Integer formatConfidence, Integer sampleSize,
                                Integer matchedSampleCount) {
        LogAnalysisEntity entity = new LogAnalysisEntity();
        entity.setFileName(job.getFileName());
        entity.setFileSize(job.getFileSize());
        entity.setAnalysisName(job.getAnalysisName());
        entity.setTotalLines(accumulator.getTotalLines());
        entity.setInfoCount(accumulator.getInfoCount());
        entity.setWarningCount(accumulator.getWarningCount());
        entity.setErrorCount(accumulator.getErrorCount());
        entity.setExceptionCount(accumulator.getExceptionCount());
        entity.setAnalyzedAt(Instant.now());
        entity.setProcessingDurationMs(Instant.now().toEpochMilli() - job.getStartedAt().toEpochMilli());

        entity.setRequestedParserType(job.getRequestedParserType());
        entity.setDetectedLogFormat(detectedFormat.name());
        entity.setDetectedEnvelope(detectedEnvelope.name());
        entity.setParsedEntryCount(accumulator.getParsedEntryCount());
        entity.setUnparsedLineCount(accumulator.getUnparsedLineCount());
        entity.setFirstLogTimestamp(accumulator.getFirstLogTimestamp());
        entity.setLastLogTimestamp(accumulator.getLastLogTimestamp());
        entity.setMultilineExceptionCount(accumulator.getMultilineExceptionCount());
        entity.setFormatConfidence(formatConfidence);
        entity.setFormatDetectionSampleSize(sampleSize);
        entity.setMatchedSampleCount(matchedSampleCount);
        entity.setParseQualityScore(accumulator.computeParseQualityScore());
        entity.setTimelineGranularity(timelineAggregator.getGranularityName());

        Map<String, String> sampleMessages = accumulator.getNormalizedErrorSampleMessages();
        accumulator.getNormalizedErrorCounts().forEach((normalizedMessage, count) ->
                entity.addFrequentError(new FrequentErrorEntity(sampleMessages.get(normalizedMessage), normalizedMessage, count)));

        LogAnalysisEntity savedEntity = logAnalysisRepository.save(entity);

        saveLoggerStats(savedEntity.getId(), accumulator);
        saveThreadStats(savedEntity.getId(), accumulator);
        saveStatusCodeStats(savedEntity.getId(), accumulator);
        saveHttpMethodStats(savedEntity.getId(), accumulator);
        timelineStatRepository.saveAll(timelineAggregator.toEntities(savedEntity.getId()));

        job.setStatus(JobStatus.SUCCEEDED);
        job.setProgress(100);
        job.setCompletedAt(Instant.now());
        job.setAnalysisId(savedEntity.getId());
        job = analysisJobRepository.save(job);

        tempFileStorageService.delete(job.getId());
    }

    private void saveLoggerStats(Long logAnalysisId, AnalysisResultAccumulator accumulator) {
        accumulator.getLoggerCounts().forEach((loggerName, count) ->
                loggerStatRepository.save(new AnalysisLoggerStatEntity(logAnalysisId, loggerName, count)));
    }

    private void saveThreadStats(Long logAnalysisId, AnalysisResultAccumulator accumulator) {
        accumulator.getThreadCounts().forEach((threadName, count) ->
                threadStatRepository.save(new AnalysisThreadStatEntity(logAnalysisId, threadName, count)));
    }

    private void saveStatusCodeStats(Long logAnalysisId, AnalysisResultAccumulator accumulator) {
        accumulator.getStatusCodeCounts().forEach((statusCode, count) ->
                statusCodeStatRepository.save(new AnalysisStatusCodeStatEntity(logAnalysisId, statusCode, count)));
    }

    private void saveHttpMethodStats(Long logAnalysisId, AnalysisResultAccumulator accumulator) {
        accumulator.getHttpMethodCounts().forEach((httpMethod, count) ->
                httpMethodStatRepository.save(new AnalysisHttpMethodStatEntity(logAnalysisId, httpMethod, count)));
    }

    private void handleFailure(AnalysisJobEntity job, String errorCode, String errorMessage) {
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(Instant.now());
        job.setErrorCode(errorCode);
        job.setErrorMessage(errorMessage);
        analysisJobRepository.save(job);
    }

    private void handleCancellation(AnalysisJobEntity job) {
        job.setStatus(JobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        job = analysisJobRepository.save(job);

        tempFileStorageService.delete(job.getId());
    }
}