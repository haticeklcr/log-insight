package com.hatice.loginsight.parser;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class LogMessageNormalizer {

    private record NormalizationRule(Pattern pattern, String replacement) {
    }

    private static final List<NormalizationRule> RULES = List.of(
            new NormalizationRule(
                    Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"),
                    "<UUID>"),
            new NormalizationRule(
                    Pattern.compile("\\b(?:[0-9a-fA-F]{1,4}:){2,7}[0-9a-fA-F]{1,4}\\b"),
                    "<IPV6>"),
            new NormalizationRule(
                    Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"),
                    "<IP>"),
            new NormalizationRule(
                    Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?\\b"),
                    "<TIMESTAMP>"),
            new NormalizationRule(
                    Pattern.compile("(?i)\\b(request[_-]?id)\\s*[:=]\\s*[\\w-]+"),
                    "$1=<REQUEST_ID>"),
            new NormalizationRule(
                    Pattern.compile("(?i)\\b(trace[_-]?id)\\s*[:=]\\s*[\\w-]+"),
                    "$1=<TRACE_ID>"),
            new NormalizationRule(
                    Pattern.compile("(?i)\\bport\\s*[:=]?\\s*\\d{2,5}\\b"),
                    "port <PORT>"),
            new NormalizationRule(
                    Pattern.compile(":\\d{2,5}\\b"),
                    ":<PORT>"),
            new NormalizationRule(
                    Pattern.compile("\\b0x[0-9a-fA-F]+\\b"),
                    "<HEX>"),
            new NormalizationRule(
                    Pattern.compile("\\b[0-9a-fA-F]{12,}\\b"),
                    "<HEX>"),
            new NormalizationRule(
                    Pattern.compile("\\b\\d+\\b"),
                    "<NUMBER>")
    );

    public String normalize(String rawMessage) {
        if (rawMessage == null) {
            return null;
        }
        String result = rawMessage;
        for (NormalizationRule rule : RULES) {
            result = rule.pattern().matcher(result).replaceAll(rule.replacement());
        }
        return result;
    }
}