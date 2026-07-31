package com.hatice.loginsight.parser;

public class MultilineExceptionInfo {

    private final String exceptionType;
    private final String exceptionMessage;
    private final String rootCauseType;
    private final String rootCauseMessage;
    private final boolean multiline;

    public MultilineExceptionInfo(String exceptionType, String exceptionMessage,
                                   String rootCauseType, String rootCauseMessage,
                                   boolean multiline) {
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
        this.rootCauseType = rootCauseType;
        this.rootCauseMessage = rootCauseMessage;
        this.multiline = multiline;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getRootCauseType() {
        return rootCauseType;
    }

    public String getRootCauseMessage() {
        return rootCauseMessage;
    }

    public boolean isMultiline() {
        return multiline;
    }
}