package com.hatice.loginsight.exception;

public class TooManyActiveUploadsException extends RuntimeException {

    public TooManyActiveUploadsException(String message) {
        super(message);
    }
}