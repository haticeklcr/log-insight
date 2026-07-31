package com.hatice.loginsight.parser;

public interface LogParser {

    
    LogFormat getFormat();

    
    boolean canParse(String rawLine);

    
    ParsedLogEntry parse(String rawLine);
}