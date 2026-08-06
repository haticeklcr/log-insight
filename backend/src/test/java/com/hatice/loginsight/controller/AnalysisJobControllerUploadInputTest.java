package com.hatice.loginsight.controller;

import com.hatice.loginsight.AbstractIntegrationTest;
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
class AnalysisJobControllerUploadInputTest extends AbstractIntegrationTest {

    @Autowired
    private AnalysisJobController analysisJobController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(analysisJobController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsRequestWithNeitherFileNorUploadId() throws Exception {
        mockMvc.perform(multipart("/api/v1/analysis-jobs")
                        .param("analysisName", "Test Analizi"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JOB_INPUT"));
    }

    @Test
    void rejectsRequestWithBothFileAndUploadId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.log", "text/plain", "log content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/analysis-jobs")
                        .file(file)
                        .param("uploadId", "3f2a91c4-6d5b-4e77-9a13-0c8ef4b21d55")
                        .param("analysisName", "Test Analizi"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JOB_INPUT"));
    }

    @Test
    void rejectsRequestWithNonExistentUploadId() throws Exception {
        mockMvc.perform(multipart("/api/v1/analysis-jobs")
                        .param("uploadId", "3f2a91c4-6d5b-4e77-9a13-0c8ef4b21d55")
                        .param("analysisName", "Test Analizi"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("UPLOAD_SESSION_NOT_FOUND"));
    }
}