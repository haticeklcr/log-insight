package com.hatice.loginsight.parser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EnvelopeDetector {

    private static final Pattern RFC3164_PATTERN = Pattern.compile(
            "^([A-Z][a-z]{2}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+"
                    + "(\\S+)\\s+"
                    + "([^:\\[\\s]+)"
                    + "(?:\\[(\\d+)])?"
                    + ":\\s(.*)$");

    private static final Pattern RFC5424_PATTERN = Pattern.compile(
            "^<(\\d{1,3})>(\\d+)\\s+"
                    + "(\\S+)\\s+"
                    + "(\\S+)\\s+"
                    + "(\\S+)\\s+"
                    + "(\\S+)\\s+"
                    + "(\\S+)\\s*"
                    + "(.*)$");

    private static final Pattern CRI_PATTERN = Pattern.compile(
            "^(\\S+)\\s+"
                    + "(stdout|stderr)\\s+"
                    + "([FP])\\s"
                    + "(.*)$");

    private static final DateTimeFormatter RFC3164_TIMESTAMP_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("MMM d HH:mm:ss")
                    .parseDefaulting(java.time.temporal.ChronoField.YEAR, Year.now().getValue())
                    .toFormatter(Locale.ENGLISH);

    private final boolean enabled;
    private final int confidenceThreshold;

    public EnvelopeDetector(@Value("${app.envelope-detection.enabled}") boolean enabled,
                             @Value("${app.envelope-detection.confidence-threshold}") int confidenceThreshold) {
        this.enabled = enabled;
        this.confidenceThreshold = confidenceThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LogEnvelopeDetectionResult detect(List<String> sampleLines) {
        if (!enabled || sampleLines.isEmpty()) {
            return new LogEnvelopeDetectionResult(LogEnvelope.NONE, 0, sampleLines.size(), 0);
        }

        LogEnvelope bestEnvelope = LogEnvelope.NONE;
        int bestMatched = 0;
        int bestConfidence = -1;

        for (LogEnvelope candidate : new LogEnvelope[]{
                LogEnvelope.SYSLOG_RFC3164, LogEnvelope.SYSLOG_RFC5424, LogEnvelope.CONTAINER_CRI}) {
            int matched = countMatches(candidate, sampleLines);
            int confidence = (int) Math.round((matched * 100.0) / sampleLines.size());
            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                bestMatched = matched;
                bestEnvelope = candidate;
            }
        }

        if (bestConfidence >= confidenceThreshold) {
            return new LogEnvelopeDetectionResult(bestEnvelope, bestConfidence, sampleLines.size(), bestMatched);
        }

        return new LogEnvelopeDetectionResult(LogEnvelope.NONE, bestConfidence, sampleLines.size(), bestMatched);
    }

    public EnvelopeStripResult strip(String rawLine, LogEnvelope envelope) {
        return switch (envelope) {
            case SYSLOG_RFC3164 -> stripRfc3164(rawLine);
            case SYSLOG_RFC5424 -> stripRfc5424(rawLine);
            case CONTAINER_CRI -> stripCri(rawLine);
            case NONE -> null;
        };
    }

    private int countMatches(LogEnvelope envelope, List<String> sampleLines) {
        int matched = 0;
        for (String line : sampleLines) {
            if (matches(envelope, line)) {
                matched++;
            }
        }
        return matched;
    }

    private boolean matches(LogEnvelope envelope, String rawLine) {
        return switch (envelope) {
            case SYSLOG_RFC3164 -> RFC3164_PATTERN.matcher(rawLine).matches();
            case SYSLOG_RFC5424 -> RFC5424_PATTERN.matcher(rawLine).matches();
            case CONTAINER_CRI -> CRI_PATTERN.matcher(rawLine).matches();
            case NONE -> false;
        };
    }

    private EnvelopeStripResult stripRfc3164(String rawLine) {
        Matcher matcher = RFC3164_PATTERN.matcher(rawLine);
        if (!matcher.matches()) {
            return null;
        }
        Instant timestamp = parseRfc3164Timestamp(matcher.group(1));
        return new EnvelopeStripResult(matcher.group(5), timestamp, null, null);
    }

    private EnvelopeStripResult stripRfc5424(String rawLine) {
        Matcher matcher = RFC5424_PATTERN.matcher(rawLine);
        if (!matcher.matches()) {
            return null;
        }
        Instant timestamp = parseRfc5424Timestamp(matcher.group(3));
        String payload = stripLeadingNilStructuredData(matcher.group(8));
        return new EnvelopeStripResult(payload, timestamp, null, null);
    }

    private EnvelopeStripResult stripCri(String rawLine) {
        Matcher matcher = CRI_PATTERN.matcher(rawLine);
        if (!matcher.matches()) {
            return null;
        }
        Instant timestamp = parseCriTimestamp(matcher.group(1));
        return new EnvelopeStripResult(matcher.group(4), timestamp, matcher.group(2), matcher.group(3));
    }

    private String stripLeadingNilStructuredData(String rest) {
        if (rest.startsWith("- ")) {
            return rest.substring(2);
        }
        if (rest.equals("-")) {
            return "";
        }
        return rest;
    }

    private Instant parseRfc3164Timestamp(String rawTimestamp) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(rawTimestamp, RFC3164_TIMESTAMP_FORMAT);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeException e) {
            return null;
        }
    }

    private Instant parseRfc5424Timestamp(String rawTimestamp) {
        try {
            return OffsetDateTime.parse(rawTimestamp).toInstant();
        } catch (DateTimeException e) {
            return null;
        }
    }

    private Instant parseCriTimestamp(String rawTimestamp) {
        try {
            return Instant.parse(rawTimestamp);
        } catch (DateTimeException e) {
            return null;
        }
    }
}