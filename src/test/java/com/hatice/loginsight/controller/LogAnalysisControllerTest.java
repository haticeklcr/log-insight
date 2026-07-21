package com.hatice.loginsight.controller;

import com.hatice.loginsight.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class LogAnalysisControllerTest {

    @Autowired
    private LogAnalysisController logAnalysisController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(logAnalysisController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    private static final String SAMPLE_LOG_CONTENT =
            "2026-01-01 10:00:00 INFO Application started\n" +
            "2026-01-01 10:00:03 ERROR: Connection refused\n";

    @Test
    void analyzeEndpointReturnsSuccessfulResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "application.log", "text/plain",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/logs/analyze").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("application.log"))
                .andExpect(jsonPath("$.totalLines").value(2));
    }

    @Test
    void analyzeEndpointReturnsBadRequestForEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.log", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/v1/logs/analyze").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyzeEndpointReturnsBadRequestForUnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "application.pdf", "application/pdf",
                SAMPLE_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/logs/analyze").file(file))
                .andExpect(status().isBadRequest());
    }
}