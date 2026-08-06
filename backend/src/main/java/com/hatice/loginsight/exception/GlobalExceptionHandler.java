package com.hatice.loginsight.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ApiError> handleEmptyFile(EmptyFileException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "EMPTY_FILE", ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ApiError> handleUnsupportedFileType(UnsupportedFileTypeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_LOG_FILE", ex.getMessage(), request);
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ApiError> handleFileTooLarge(FileTooLargeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", ex.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "Yüklenen dosya izin verilen maksimum boyutu aşıyor", request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "İstek bir dosya (file) alanı içermeli", request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleMultipartException(MultipartException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "İstek multipart/form-data formatında olmalı ve bir dosya içermeli", request);
    }

    @ExceptionHandler(AnalysisNotFoundException.class)
    public ResponseEntity<ApiError> handleAnalysisNotFound(AnalysisNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidAnalysisNameException.class)
    public ResponseEntity<ApiError> handleInvalidAnalysisName(InvalidAnalysisNameException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_ANALYSIS_NAME", ex.getMessage(), request);
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ApiError> handleJobNotFound(JobNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidJobStateException.class)
    public ResponseEntity<ApiError> handleInvalidJobState(InvalidJobStateException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "INVALID_JOB_STATE", ex.getMessage(), request);
    }

    @ExceptionHandler(JobRetryLimitExceededException.class)
    public ResponseEntity<ApiError> handleRetryLimitExceeded(JobRetryLimitExceededException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "RETRY_LIMIT_EXCEEDED", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidParserTypeException.class)
    public ResponseEntity<ApiError> handleInvalidParserType(InvalidParserTypeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_PARSER_TYPE", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiError> handleInvalidDateRange(InvalidDateRangeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedFilterForParserException.class)
    public ResponseEntity<ApiError> handleUnsupportedFilterForParser(UnsupportedFilterForParserException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILTER_FOR_PARSER", ex.getMessage(), request);
    }

    @ExceptionHandler(UploadSessionNotFoundException.class)
    public ResponseEntity<ApiError> handleUploadSessionNotFound(UploadSessionNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "UPLOAD_SESSION_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(UploadSessionExpiredException.class)
    public ResponseEntity<ApiError> handleUploadSessionExpired(UploadSessionExpiredException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "UPLOAD_SESSION_EXPIRED", ex.getMessage(), request);
    }

    @ExceptionHandler(UploadSessionNotInProgressException.class)
    public ResponseEntity<ApiError> handleUploadSessionNotInProgress(UploadSessionNotInProgressException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "UPLOAD_SESSION_NOT_IN_PROGRESS", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidChunkIndexException.class)
    public ResponseEntity<ApiError> handleInvalidChunkIndex(InvalidChunkIndexException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_CHUNK_INDEX", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidChunkSizeException.class)
    public ResponseEntity<ApiError> handleInvalidChunkSize(InvalidChunkSizeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_CHUNK_SIZE", ex.getMessage(), request);
    }

    @ExceptionHandler(UploadIncompleteException.class)
    public ResponseEntity<ApiError> handleUploadIncomplete(UploadIncompleteException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "UPLOAD_INCOMPLETE", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidUploadRequestException.class)
    public ResponseEntity<ApiError> handleInvalidUploadRequest(InvalidUploadRequestException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(ChunkAlreadyExistsWithDifferentSizeException.class)
    public ResponseEntity<ApiError> handleChunkAlreadyExistsWithDifferentSize(ChunkAlreadyExistsWithDifferentSizeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "CHUNK_ALREADY_EXISTS_WITH_DIFFERENT_SIZE", ex.getMessage(), request);
    }
    
    @ExceptionHandler(InsufficientDiskSpaceException.class)
    public ResponseEntity<ApiError> handleInsufficientDiskSpace(InsufficientDiskSpaceException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INSUFFICIENT_STORAGE, "INSUFFICIENT_DISK_SPACE", ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyActiveUploadsException.class)
    public ResponseEntity<ApiError> handleTooManyActiveUploads(TooManyActiveUploadsException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ACTIVE_UPLOADS", ex.getMessage(), request);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String errorCode, String message,
                                                     HttpServletRequest request) {
        ApiError apiError = new ApiError(
                Instant.now().toString(),
                status.value(),
                errorCode,
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(apiError);
    }
}