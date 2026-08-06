package com.hatice.loginsight.exception;

public class ChunkAlreadyExistsWithDifferentSizeException extends RuntimeException {

    public ChunkAlreadyExistsWithDifferentSizeException(String message) {
        super(message);
    }
}