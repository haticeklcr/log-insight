package com.hatice.loginsight.service;

import com.hatice.loginsight.dto.AnalysisDetailDto;
import com.hatice.loginsight.dto.AnalysisSummaryDto;
import com.hatice.loginsight.dto.AppliedFiltersDto;
import com.hatice.loginsight.dto.ErrorFrequency;
import com.hatice.loginsight.dto.HttpMethodCount;
import com.hatice.loginsight.dto.LoggerFrequency;
import com.hatice.loginsight.dto.PagedResponse;
import com.hatice.loginsight.dto.StatusCodeCount;
import com.hatice.loginsight.dto.ThreadFrequency;
import com.hatice.loginsight.dto.TimelineBucketDto;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.exception.AnalysisNotFoundException;
import com.hatice.loginsight.repository.AnalysisHttpMethodStatRepository;
import com.hatice.loginsight.repository.AnalysisJobRepository;
import com.hatice.loginsight.repository.AnalysisLoggerStatRepository;
import com.hatice.loginsight.repository.AnalysisStatusCodeStatRepository;
import com.hatice.loginsight.repository.AnalysisThreadStatRepository;
import com.hatice.loginsight.repository.AnalysisTimelineStatRepository;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalysisHistoryService {

    private final LogAnalysisRepository logAnalysisRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnalysisLoggerStatRepository loggerStatRepository;
    private final AnalysisThreadStatRepository threadStatRepository;
    private final AnalysisStatusCodeStatRepository statusCodeStatRepository;
    private final AnalysisHttpMethodStatRepository httpMethodStatRepository;
    private final AnalysisTimelineStatRepository timelineStatRepository;

    public AnalysisHistoryService(LogAnalysisRepository logAnalysisRepository,
                                   AnalysisJobRepository analysisJobRepository,
                                   AnalysisLoggerStatRepository loggerStatRepository,
                                   AnalysisThreadStatRepository threadStatRepository,
                                   AnalysisStatusCodeStatRepository statusCodeStatRepository,
                                   AnalysisHttpMethodStatRepository httpMethodStatRepository,
                                   AnalysisTimelineStatRepository timelineStatRepository) {
        this.logAnalysisRepository = logAnalysisRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.loggerStatRepository = loggerStatRepository;
        this.threadStatRepository = threadStatRepository;
        this.statusCodeStatRepository = statusCodeStatRepository;
        this.httpMethodStatRepository = httpMethodStatRepository;
        this.timelineStatRepository = timelineStatRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AnalysisSummaryDto> listAnalyses(int page, int size, String sortField, String sortDirection,
                                                           String fileName, String analysisName, Integer minErrorCount) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<LogAnalysisEntity> specification = buildSpecification(fileName, analysisName, minErrorCount);

        Page<LogAnalysisEntity> entityPage = logAnalysisRepository.findAll(specification, pageable);

        List<AnalysisSummaryDto> content = entityPage.getContent().stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.isFirst(),
                entityPage.isLast());
    }

    @Transactional(readOnly = true)
    public AnalysisDetailDto getAnalysisDetail(Long id) {
        LogAnalysisEntity entity = logAnalysisRepository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException("ID " + id + " için analiz kaydı bulunamadı"));
        return toDetailDto(entity);
    }

    @Transactional
    public void deleteAnalysis(Long id) {
        if (!logAnalysisRepository.existsById(id)) {
            throw new AnalysisNotFoundException("ID " + id + " için analiz kaydı bulunamadı");
        }
        logAnalysisRepository.deleteById(id);
    }

    private Specification<LogAnalysisEntity> buildSpecification(String fileName, String analysisName, Integer minErrorCount) {
        Specification<LogAnalysisEntity> specification = (root, query, cb) -> cb.conjunction();

        if (fileName != null && !fileName.isBlank()) {
            String pattern = "%" + fileName.toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("fileName")), pattern));
        }

        if (analysisName != null && !analysisName.isBlank()) {
            String pattern = "%" + analysisName.toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("analysisName")), pattern));
        }

        if (minErrorCount != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("errorCount"), minErrorCount));
        }

        return specification;
    }

    private AnalysisSummaryDto toSummaryDto(LogAnalysisEntity entity) {
        return new AnalysisSummaryDto(
                entity.getId(),
                entity.getFileName(),
                entity.getAnalysisName(),
                entity.getFileSize(),
                entity.getAnalyzedAt(),
                entity.getTotalLines(),
                entity.getErrorCount(),
                entity.getExceptionCount(),
                entity.getProcessingDurationMs());
    }

    private AnalysisDetailDto toDetailDto(LogAnalysisEntity entity) {
        AnalysisDetailDto dto = new AnalysisDetailDto();
        dto.setId(entity.getId());
        dto.setFileName(entity.getFileName());
        dto.setAnalysisName(entity.getAnalysisName());
        dto.setFileSize(entity.getFileSize());
        dto.setAnalyzedAt(entity.getAnalyzedAt());
        dto.setProcessingDurationMs(entity.getProcessingDurationMs());
        dto.setTotalLines(entity.getTotalLines());
        dto.setInfoCount(entity.getInfoCount());
        dto.setWarningCount(entity.getWarningCount());
        dto.setErrorCount(entity.getErrorCount());
        dto.setExceptionCount(entity.getExceptionCount());

        List<ErrorFrequency> frequentErrors = entity.getFrequentErrors().stream()
                .sorted((a, b) -> b.getOccurrenceCount() - a.getOccurrenceCount())
                .map(fe -> new ErrorFrequency(fe.getMessage(), fe.getNormalizedMessage(), fe.getOccurrenceCount()))
                .collect(Collectors.toList());
        dto.setMostFrequentErrors(frequentErrors);

        dto.setRequestedParserType(entity.getRequestedParserType());
        dto.setDetectedLogFormat(entity.getDetectedLogFormat());
        dto.setParsedEntryCount(entity.getParsedEntryCount());
        dto.setUnparsedLineCount(entity.getUnparsedLineCount());
        dto.setUnparsedLinePercentage(computeUnparsedLinePercentage(entity));
        dto.setFirstLogTimestamp(entity.getFirstLogTimestamp());
        dto.setLastLogTimestamp(entity.getLastLogTimestamp());
        dto.setMultilineExceptionCount(entity.getMultilineExceptionCount());
        dto.setParseQualityScore(entity.getParseQualityScore());
        dto.setFormatConfidence(entity.getFormatConfidence());
        dto.setFormatDetectionSampleSize(entity.getFormatDetectionSampleSize());
        dto.setMatchedSampleCount(entity.getMatchedSampleCount());

        dto.setMostFrequentLoggers(loggerStatRepository.findByLogAnalysisId(entity.getId()).stream()
                .map(s -> new LoggerFrequency(s.getLoggerName(), s.getEntryCount()))
                .collect(Collectors.toList()));

        dto.setMostFrequentThreads(threadStatRepository.findByLogAnalysisId(entity.getId()).stream()
                .map(s -> new ThreadFrequency(s.getThreadName(), s.getEntryCount()))
                .collect(Collectors.toList()));

        dto.setStatusCodeDistribution(statusCodeStatRepository.findByLogAnalysisId(entity.getId()).stream()
                .map(s -> new StatusCodeCount(s.getStatusCode(), s.getEntryCount()))
                .collect(Collectors.toList()));

        dto.setHttpMethodDistribution(httpMethodStatRepository.findByLogAnalysisId(entity.getId()).stream()
                .map(s -> new HttpMethodCount(s.getHttpMethod(), s.getEntryCount()))
                .collect(Collectors.toList()));

        dto.setTimeline(timelineStatRepository.findByLogAnalysisIdOrderByBucketStartAsc(entity.getId()).stream()
                .map(s -> new TimelineBucketDto(s.getBucketStart(), s.getTotalCount(), s.getInfoCount(),
                        s.getWarnCount(), s.getErrorCount(), s.getExceptionCount()))
                .collect(Collectors.toList()));

        analysisJobRepository.findByAnalysisId(entity.getId())
                .ifPresent(job -> dto.setAppliedFilters(AppliedFiltersDto.from(job)));

        return dto;
    }

    private Double computeUnparsedLinePercentage(LogAnalysisEntity entity) {
        Integer parsed = entity.getParsedEntryCount();
        Integer unparsed = entity.getUnparsedLineCount();
        if (parsed == null || unparsed == null) {
            return null;
        }
        int total = parsed + unparsed;
        if (total == 0) {
            return 0.0;
        }
        return (unparsed * 100.0) / total;
    }
}