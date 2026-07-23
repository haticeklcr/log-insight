package com.hatice.loginsight.controller;

import com.hatice.loginsight.dto.AnalysisDetailDto;
import com.hatice.loginsight.dto.AnalysisSummaryDto;
import com.hatice.loginsight.dto.PagedResponse;
import com.hatice.loginsight.service.AnalysisHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisHistoryController {

    private final AnalysisHistoryService analysisHistoryService;

    public AnalysisHistoryController(AnalysisHistoryService analysisHistoryService) {
        this.analysisHistoryService = analysisHistoryService;
    }

    @GetMapping
    public PagedResponse<AnalysisSummaryDto> listAnalyses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "analyzedAt,desc") String sort,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) Integer minErrorCount) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        String sortDirection = sortParts.length > 1 ? sortParts[1] : "desc";

        return analysisHistoryService.listAnalyses(page, size, sortField, sortDirection, fileName, minErrorCount);
    }

    @GetMapping("/{id}")
    public AnalysisDetailDto getAnalysis(@PathVariable Long id) {
        return analysisHistoryService.getAnalysisDetail(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Long id) {
        analysisHistoryService.deleteAnalysis(id);
        return ResponseEntity.noContent().build();
    }
}