package com.hatice.loginsight.parser;

import org.springframework.stereotype.Component;

@Component
public class NginxAccessLogParser implements LogParser {

    @Override
    public LogFormat getFormat() {
        return LogFormat.NGINX_ACCESS;
    }

    @Override
    public boolean canParse(String rawLine) {
        return HttpAccessLogFormat.COMMON_PATTERN.matcher(rawLine).matches();
    }

    @Override
    public ParsedLogEntry parse(String rawLine) {
        return HttpAccessLogFormat.parseCommon(rawLine, LogFormat.NGINX_ACCESS);
    }
}