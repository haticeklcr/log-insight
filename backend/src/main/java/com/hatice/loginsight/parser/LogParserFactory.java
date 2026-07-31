package com.hatice.loginsight.parser;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class LogParserFactory {

    private final Map<LogFormat, LogParser> parsersByFormat = new EnumMap<>(LogFormat.class);
    private final List<LogParser> allParsers;

    public LogParserFactory(List<LogParser> parsers) {
        this.allParsers = parsers;
        for (LogParser parser : parsers) {
            parsersByFormat.put(parser.getFormat(), parser);
        }
    }

    public LogParser getParser(LogFormat format) {
        LogParser parser = parsersByFormat.get(format);
        if (parser == null) {
            throw new IllegalArgumentException("Desteklenmeyen log formatı: " + format);
        }
        return parser;
    }

    public List<LogParser> getAllParsers() {
        return allParsers;
    }
}