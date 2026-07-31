package com.hatice.loginsight.parser;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;


@Component
public class JsonLogParser implements LogParser {

    private static final String[] TIMESTAMP_FIELDS = {"timestamp", "time", "@timestamp"};
    private static final String[] LEVEL_FIELDS = {"level", "logLevel", "severity"};
    private static final String[] LOGGER_FIELDS = {"logger", "class", "source"};
    private static final String[] MESSAGE_FIELDS = {"message", "msg"};
    private static final String[] THREAD_FIELDS = {"thread", "threadName"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LogFormat getFormat() {
        return LogFormat.JSON;
    }

    @Override
    public boolean canParse(String rawLine) {
        String trimmed = rawLine.trim();
        if (!trimmed.startsWith("{")) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            return node.isObject();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ParsedLogEntry parse(String rawLine) {
        String trimmed = rawLine.trim();
        JsonNode node;
        try {
            node = objectMapper.readTree(trimmed);
        } catch (Exception e) {
            return null;
        }
        if (!node.isObject()) {
            return null;
        }

        ParsedLogEntry entry = new ParsedLogEntry();
        entry.setSourceFormat(LogFormat.JSON);
        entry.setRawLine(rawLine);

        entry.setTimestamp(parseTimestamp(firstNonNull(node, TIMESTAMP_FIELDS)));
        entry.setLevel(firstNonNull(node, LEVEL_FIELDS));
        entry.setLogger(firstNonNull(node, LOGGER_FIELDS));
        entry.setMessage(firstNonNull(node, MESSAGE_FIELDS));
        entry.setThread(firstNonNull(node, THREAD_FIELDS));

        return entry;
    }

    
    private String firstNonNull(JsonNode node, String[] candidateFields) {
        for (String field : candidateFields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.isTextual() ? value.asText() : value.toString();
            }
        }
        return null;
    }

    
    private Instant parseTimestamp(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(rawValue);
        } catch (DateTimeParseException ignored) {
            // ISO-8601 offset (Z olmadan +03:00 gibi) formatını dene
        }
        try {
            return OffsetDateTime.parse(rawValue).toInstant();
        } catch (DateTimeParseException ignored) {
            // Epoch milisaniye (sayısal string) olabilir
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(rawValue));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}