package com.hatice.loginsight.controller;

import com.hatice.loginsight.dto.LogAnalysisResult;
import com.hatice.loginsight.service.LogAnalysisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/logs")
public class LogAnalysisController {

    private final LogAnalysisService logAnalysisService;

    public LogAnalysisController(LogAnalysisService logAnalysisService) {
        this.logAnalysisService = logAnalysisService;
    }

    @PostMapping("/analyze")
    public LogAnalysisResult analyze(@RequestParam("file") MultipartFile file) throws IOException{
        return logAnalysisService.analyze(file);
    }
    
}
