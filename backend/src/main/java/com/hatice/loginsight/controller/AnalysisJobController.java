package com.hatice.loginsight.controller;

import com.hatice.loginsight.dto.AnalysisJobDetailDto;
import com.hatice.loginsight.dto.CreateAnalysisJobResponse;
import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.service.AnalysisJobService;
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
    public CreateAnalysisJobResponse createJob(@RequestParam("file") MultipartFile file,
                                                @RequestParam("analysisName") String analysisName) {
        AnalysisJobEntity job = analysisJobService.createJob(file, analysisName);
        return new CreateAnalysisJobResponse(
                job.getId(), job.getAnalysisName(), job.getStatus(), job.getProgress(), job.getCreatedAt());
    }

    @GetMapping("/{id}")
    public AnalysisJobDetailDto getJob(@PathVariable UUID id) {
        return toDetailDto(analysisJobService.getJob(id));
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
        return dto;
    }
}