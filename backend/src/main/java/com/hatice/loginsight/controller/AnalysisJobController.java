package com.hatice.loginsight.controller;

import com.hatice.loginsight.dto.CreateAnalysisJobResponse;
import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.service.AnalysisJobService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}