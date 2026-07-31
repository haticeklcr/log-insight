package com.hatice.loginsight.parser;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PlainTextLogParser implements LogParser {

    private static final Pattern LEVEL_PATTERN =
            Pattern.compile("\\b(TRACE|DEBUG|INFO|WARNING|WARN|ERROR)\\b");

    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile("(Caused by|Exception)");

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
        entry.setTimestamp(null);

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
}