package com.hatice.loginsight.parser;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PlainTextLogParser implements LogParser {

    private static final Pattern LEVEL_PATTERN =
            Pattern.compile("\\b(TRACE|DEBUG|INFO|WARNING|WARN|ERROR)\\b");

    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile("(Caused by|Exception)");

    private static final Pattern LEADING_TIMESTAMP_PATTERN =
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?)");

    private static final DateTimeFormatter LEADING_TIMESTAMP_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd[ ]['T']HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 3, true)
            .toFormatter();

    @Override
    public LogFormat getFormat() {
        return LogFormat.PLAIN_TEXT;
    }

    @Override
    public boolean canParse(String rawLine) {
        return true;
    }

    @Override
    public ParsedLogEntry parse(String rawLine) {
        ParsedLogEntry entry = new ParsedLogEntry();
        entry.setSourceFormat(LogFormat.PLAIN_TEXT);
        entry.setRawLine(rawLine);
        entry.setTimestamp(extractLeadingTimestamp(rawLine));

        Matcher levelMatcher = LEVEL_PATTERN.matcher(rawLine);
        if (levelMatcher.find()) {
            String level = levelMatcher.group(1);
            // WARNING ve WARN aynı seviyeyi temsil eder; downstream
            // katmanların (istatistikler, filtreler) tek bir isimle
            // çalışabilmesi için WARNING -> WARN olarak normalize ediyoruz.
            entry.setLevel("WARNING".equals(level) ? "WARN" : level);

            String message = rawLine.substring(levelMatcher.end()).trim();
            if (message.startsWith(":")) {
                message = message.substring(1).trim();
            }
            entry.setMessage(message);
        } else {
            entry.setMessage(rawLine.trim());
        }

        if (EXCEPTION_PATTERN.matcher(rawLine).find()) {
            entry.setExceptionType(extractExceptionType(rawLine));
        }

        return entry;
    }

    private String extractExceptionType(String rawLine) {
        Matcher matcher = Pattern.compile("([\\w.$]+Exception)").matcher(rawLine);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Instant extractLeadingTimestamp(String rawLine) {
        Matcher matcher = LEADING_TIMESTAMP_PATTERN.matcher(rawLine.trim());
        if (!matcher.find()) {
            return null;
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(matcher.group(1), LEADING_TIMESTAMP_FORMAT);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}