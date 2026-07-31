package com.hatice.loginsight.parser;

import org.springframework.stereotype.Component;

@Component
public class ApacheAccessLogParser implements LogParser {

    @Override
    public LogFormat getFormat() {
        return LogFormat.APACHE_ACCESS;
    }

    @Override
    public boolean canParse(String rawLine) {
        return HttpAccessLogFormat.COMBINED_PATTERN.matcher(rawLine).matches();
    }

    @Override
    public ParsedLogEntry parse(String rawLine) {
        return HttpAccessLogFormat.parseCombined(rawLine, LogFormat.APACHE_ACCESS);
    }
}