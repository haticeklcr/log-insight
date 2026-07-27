package com.hatice.loginsight.exception;

public class JobRetryLimitExceededException extends RuntimeException {

    public JobRetryLimitExceededException(String message) {
        super(message);
    }
}