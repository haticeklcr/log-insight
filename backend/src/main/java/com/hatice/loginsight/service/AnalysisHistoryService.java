package com.hatice.loginsight.service;

import com.hatice.loginsight.dto.AnalysisDetailDto;
import com.hatice.loginsight.dto.AnalysisSummaryDto;
import com.hatice.loginsight.dto.ErrorFrequency;
import com.hatice.loginsight.dto.PagedResponse;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.exception.AnalysisNotFoundException;
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

    public AnalysisHistoryService(LogAnalysisRepository logAnalysisRepository) {
        this.logAnalysisRepository = logAnalysisRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AnalysisSummaryDto> listAnalyses(int page, int size, String sortField, String sortDirection,
                                                           String fileName, Integer minErrorCount) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<LogAnalysisEntity> specification = buildSpecification(fileName, minErrorCount);

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

    private Specification<LogAnalysisEntity> buildSpecification(String fileName, Integer minErrorCount) {
        Specification<LogAnalysisEntity> specification = (root, query, cb) -> cb.conjunction();

        if (fileName != null && !fileName.isBlank()) {
            String pattern = "%" + fileName.toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("fileName")), pattern));
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
                .map(fe -> new ErrorFrequency(fe.getMessage(), fe.getOccurrenceCount()))
                .collect(Collectors.toList());
        dto.setMostFrequentErrors(frequentErrors);

        return dto;
    }
}