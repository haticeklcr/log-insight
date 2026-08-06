package com.hatice.loginsight.exception;

public class UploadSessionExpiredException extends RuntimeException {

    public UploadSessionExpiredException(String message) {
        super(message);
    }
}