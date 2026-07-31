package com.hatice.loginsight.parser;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpringBootLogParser implements LogParser {

    private static final Pattern PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+" // 1: timestamp
                    + "(\\S+)\\s+"                                     // 2: level
                    + "(\\d+)\\s+---\\s+"                               // 3: processId
                    + "\\[([^\\]]*)]\\s+"                               // 4: thread
                    + "(\\S+)\\s*:\\s*"                                 // 5: logger
                    + "(.*)$");                                          // 6: message

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public LogFormat getFormat() {
        return LogFormat.SPRING_BOOT;
    }

    @Override
    public boolean canParse(String rawLine) {
        return PATTERN.matcher(rawLine).matches();
    }

    @Override
    public ParsedLogEntry parse(String rawLine) {
        Matcher matcher = PATTERN.matcher(rawLine);
        if (!matcher.matches()) {
            return null;
        }

        ParsedLogEntry entry = new ParsedLogEntry();
        entry.setSourceFormat(LogFormat.SPRING_BOOT);
        entry.setRawLine(rawLine);

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(matcher.group(1), TIMESTAMP_FORMAT);
            entry.setTimestamp(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            entry.setTimestamp(null);
        }

        entry.setLevel(matcher.group(2));
        entry.setThread(matcher.group(4));
        entry.setLogger(matcher.group(5));
        entry.setMessage(matcher.group(6).trim());

        return entry;
    }
}