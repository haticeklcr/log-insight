package com.hatice.loginsight.controller;

import com.hatice.loginsight.dto.AnalysisJobDetailDto;
import com.hatice.loginsight.dto.AnalysisJobSummaryDto;
import com.hatice.loginsight.dto.AppliedFiltersDto;
import com.hatice.loginsight.dto.CreateAnalysisJobResponse;
import com.hatice.loginsight.dto.PagedResponse;
import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import com.hatice.loginsight.exception.InvalidJobInputException;
import com.hatice.loginsight.service.AnalysisJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis-jobs")
public class AnalysisJobController {

    private final AnalysisJobService analysisJobService;

    public AnalysisJobController(AnalysisJobService analysisJobService) {
        this.analysisJobService = analysisJobService;
    }

    @PostMapping
    public CreateAnalysisJobResponse createJob(@RequestParam(value = "file", required = false) MultipartFile file,
                                                @RequestParam(value = "uploadId", required = false) UUID uploadId,
                                                @RequestParam("analysisName") String analysisName,
                                                @RequestParam(value = "parserType", required = false) String parserType,
                                                @RequestParam(value = "startTime", required = false) String startTime,
                                                @RequestParam(value = "endTime", required = false) String endTime,
                                                @RequestParam(value = "levels", required = false) String levels,
                                                @RequestParam(value = "logger", required = false) String logger,
                                                @RequestParam(value = "thread", required = false) String thread,
                                                @RequestParam(value = "messageContains", required = false) String messageContains,
                                                @RequestParam(value = "statusCodes", required = false) String statusCodes,
                                                @RequestParam(value = "httpMethods", required = false) String httpMethods,
                                                @RequestParam(value = "pathContains", required = false) String pathContains) {
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasUploadId = uploadId != null;
        if (hasFile == hasUploadId) {
            throw new InvalidJobInputException(
                    "İstek tam olarak bir tanesini içermeli: file veya uploadId");
        }

        AnalysisJobEntity job = hasFile
                ? analysisJobService.createJob(file, analysisName, parserType, startTime, endTime,
                        levels, logger, thread, messageContains, statusCodes, httpMethods, pathContains)
                : analysisJobService.createJobFromUpload(uploadId, analysisName, parserType, startTime, endTime,
                        levels, logger, thread, messageContains, statusCodes, httpMethods, pathContains);

        return new CreateAnalysisJobResponse(
                job.getId(), job.getAnalysisName(), job.getStatus(), job.getProgress(), job.getCreatedAt());
    }

    @GetMapping("/{id}")
    public AnalysisJobDetailDto getJob(@PathVariable UUID id) {
        return toDetailDto(analysisJobService.getJob(id));
    }

    @GetMapping
    public PagedResponse<AnalysisJobSummaryDto> listJobs(
            @RequestParam(required = false) String analysisName,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) JobStatus status,
            Pageable pageable) {
        Page<AnalysisJobEntity> page = analysisJobService.listJobs(analysisName, fileName, status, pageable);
        Page<AnalysisJobSummaryDto> dtoPage = page.map(this::toSummaryDto);
        return new PagedResponse<>(
                dtoPage.getContent(), dtoPage.getNumber(), dtoPage.getSize(),
                dtoPage.getTotalElements(), dtoPage.getTotalPages(), dtoPage.isFirst(), dtoPage.isLast());
    }

    @PostMapping("/{id}/cancel")
    public AnalysisJobDetailDto cancelJob(@PathVariable UUID id) {
        return toDetailDto(analysisJobService.cancelJob(id));
    }

    @PostMapping("/{id}/retry")
    public AnalysisJobDetailDto retryJob(@PathVariable UUID id) {
        return toDetailDto(analysisJobService.retryJob(id));
    }

    private AnalysisJobDetailDto toDetailDto(AnalysisJobEntity job) {
        AnalysisJobDetailDto dto = new AnalysisJobDetailDto();
        dto.setJobId(job.getId());
        dto.setAnalysisName(job.getAnalysisName());
        dto.setFileName(job.getFileName());
        dto.setFileSize(job.getFileSize());
        dto.setStatus(job.getStatus());
        dto.setProgress(job.getProgress());
        dto.setRetryCount(job.getRetryCount());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setStartedAt(job.getStartedAt());
        dto.setCompletedAt(job.getCompletedAt());
        dto.setErrorCode(job.getErrorCode());
        dto.setAnalysisId(job.getAnalysisId());
        dto.setRequestedParserType(job.getRequestedParserType());
        dto.setDetectedLogFormat(job.getDetectedLogFormat());
        dto.setDetectedEnvelope(job.getDetectedEnvelope());
        dto.setAppliedFilters(AppliedFiltersDto.from(job));
        return dto;
    }

    private AnalysisJobSummaryDto toSummaryDto(AnalysisJobEntity job) {
        AnalysisJobSummaryDto dto = new AnalysisJobSummaryDto();
        dto.setJobId(job.getId());
        dto.setAnalysisName(job.getAnalysisName());
        dto.setFileName(job.getFileName());
        dto.setStatus(job.getStatus());
        dto.setProgress(job.getProgress());
        dto.setRetryCount(job.getRetryCount());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setStartedAt(job.getStartedAt());
        dto.setCompletedAt(job.getCompletedAt());
        dto.setAnalysisId(job.getAnalysisId());
        return dto;
    }
}