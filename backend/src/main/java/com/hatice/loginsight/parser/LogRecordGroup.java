package com.hatice.loginsight.parser;

import java.util.Collections;
import java.util.List;

public class LogRecordGroup {

    private final String headerLine;
    private final List<String> continuationLines;
    private final boolean truncated;

    public LogRecordGroup(String headerLine, List<String> continuationLines, boolean truncated) {
        this.headerLine = headerLine;
        this.continuationLines = Collections.unmodifiableList(continuationLines);
        this.truncated = truncated;
    }

    public String getHeaderLine() {
        return headerLine;
    }

    public List<String> getContinuationLines() {
        return continuationLines;
    }

    public boolean isTruncated() {
        return truncated;
    }
}