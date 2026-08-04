package com.hatice.loginsight.parser;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HttpAccessLogFormat {

    static final Pattern COMMON_PATTERN = Pattern.compile(
            "^(\\S+) \\S+ \\S+ \\[([^]]+)] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\S+)$");

    static final Pattern COMBINED_PATTERN = Pattern.compile(
            "^(\\S+) \\S+ \\S+ \\[([^]]+)] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\S+) \"([^\"]*)\" \"([^\"]*)\"$");

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    private HttpAccessLogFormat() {
    }

    static ParsedLogEntry parseCommon(String rawLine, LogFormat sourceFormat) {
        Matcher matcher = COMMON_PATTERN.matcher(rawLine);
        if (!matcher.matches()) {
            return null;
        }
        return build(sourceFormat, rawLine, matcher.group(1), matcher.group(2), matcher.group(3),
                matcher.group(4), matcher.group(5), matcher.group(6), matcher.group(7), null, null);
    }

    static ParsedLogEntry parseCombined(String rawLine, LogFormat sourceFormat) {
        Matcher matcher = COMBINED_PATTERN.matcher(rawLine);
        if (!matcher.matches()) {
            return null;
        }
        return build(sourceFormat, rawLine, matcher.group(1), matcher.group(2), matcher.group(3),
                matcher.group(4), matcher.group(5), matcher.group(6), matcher.group(7),
                matcher.group(8), matcher.group(9));
    }

    private static ParsedLogEntry build(LogFormat sourceFormat, String rawLine, String clientIp,
                                         String timestampRaw, String method, String path, String protocol,
                                         String statusCodeRaw, String responseSizeRaw, String referrer,
                                         String userAgent) {
        ParsedLogEntry entry = new ParsedLogEntry();
        entry.setSourceFormat(sourceFormat);
        entry.setRawLine(rawLine);
        entry.setClientIp(clientIp);
        entry.setMethod(method);
        entry.setPath(path);
        entry.setProtocol(protocol);
        entry.setMessage(method + " " + path + " " + protocol);
        entry.setReferrer(referrer);
        entry.setUserAgent(userAgent);

        try {
            entry.setTimestamp(parseTimestamp(timestampRaw));
        } catch (Exception e) {
            entry.setTimestamp(null);
        }

        try {
            int statusCode = Integer.parseInt(statusCodeRaw);
            entry.setStatusCode(statusCode);
            entry.setLevel(deriveLevelFromStatusCode(statusCode));
        } catch (NumberFormatException e) {
            entry.setStatusCode(null);
            entry.setLevel(null);
        }

        try {
            entry.setResponseSize(Long.parseLong(responseSizeRaw));
        } catch (NumberFormatException e) {
            entry.setResponseSize(null);
        }

        return entry;
    }

    private static Instant parseTimestamp(String rawValue) {
        return ZonedDateTime.parse(rawValue, TIMESTAMP_FORMAT).toInstant();
    }

    private static String deriveLevelFromStatusCode(int statusCode) {
        if (statusCode >= 500) {
            return "ERROR";
        }
        if (statusCode >= 400) {
            return "WARN";
        }
        return "INFO";
    }
}