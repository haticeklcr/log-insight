package com.hatice.loginsight.controller;

import com.hatice.loginsight.dto.CreateUploadSessionRequest;
import com.hatice.loginsight.dto.CreateUploadSessionResponse;
import com.hatice.loginsight.dto.UploadSessionStatusResponse;
import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.service.UploadSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private final UploadSessionService uploadSessionService;

    public UploadController(UploadSessionService uploadSessionService) {
        this.uploadSessionService = uploadSessionService;
    }

    @PostMapping
    public ResponseEntity<CreateUploadSessionResponse> createSession(@RequestBody CreateUploadSessionRequest request) {
        UploadSessionEntity session = uploadSessionService.createSession(request.getFileName(), request.getFileSize());
        CreateUploadSessionResponse response = new CreateUploadSessionResponse(
                session.getId(), session.getChunkSize(), session.getTotalChunks(),
                uploadSessionService.getParallelism(), session.getExpiresAt());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{uploadId}/chunks/{chunkIndex}")
    public ResponseEntity<Void> uploadChunk(@PathVariable UUID uploadId, @PathVariable long chunkIndex,
                                             @RequestBody byte[] content) {
        uploadSessionService.uploadChunk(uploadId, chunkIndex, content);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{uploadId}")
    public ResponseEntity<UploadSessionStatusResponse> getStatus(@PathVariable UUID uploadId) {
        return ResponseEntity.ok(uploadSessionService.getStatus(uploadId));
    }

    @PostMapping("/{uploadId}/complete")
    public ResponseEntity<Void> complete(@PathVariable UUID uploadId) {
        uploadSessionService.completeSession(uploadId);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{uploadId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID uploadId) {
        uploadSessionService.cancelSession(uploadId);
        return ResponseEntity.noContent().build();
    }
}