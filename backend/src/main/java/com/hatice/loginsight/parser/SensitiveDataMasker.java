package com.hatice.loginsight.parser;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class SensitiveDataMasker {

    private record MaskingRule(Pattern pattern, String replacement) {
    }

    private static final List<MaskingRule> RULES = List.of(
            new MaskingRule(
                    Pattern.compile("(?i)(Authorization\\s*:\\s*Bearer\\s+)\\S+"),
                    "$1****"),
            new MaskingRule(
                    Pattern.compile("(?i)(Authorization\\s*:\\s*Basic\\s+)\\S+"),
                    "$1****"),
            new MaskingRule(
                    Pattern.compile("(?i)\\bBearer\\s+\\S+"),
                    "Bearer ****"),
            new MaskingRule(
                    Pattern.compile("(?i)(Cookie\\s*:\\s*)\\S.*$"),
                    "$1****"),
            new MaskingRule(
                    Pattern.compile("(?i)\\b(session[_-]?id)\\s*[:=]\\s*[\\w-]+"),
                    "$1=****"),
            new MaskingRule(
                    Pattern.compile("(?i)\\b(api[_-]?key)\\s*[:=]\\s*\\S+"),
                    "$1=****"),
            new MaskingRule(
                    Pattern.compile("(?i)\\b(access[_-]?token)\\s*[:=]\\s*\\S+"),
                    "$1=****"),
            new MaskingRule(
                    Pattern.compile("(?i)\\b(refresh[_-]?token)\\s*[:=]\\s*\\S+"),
                    "$1=****"),
            new MaskingRule(
                    Pattern.compile("(?i)\\b(password|passwd|secret)\\s*[:=]\\s*\\S+"),
                    "$1=****"),
            new MaskingRule(
                    Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}\\b"),
                    "****"),
            new MaskingRule(
                    Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{1,4}\\b"),
                    "****")
    );

    public String mask(String rawMessage) {
        if (rawMessage == null) {
            return null;
        }
        String result = rawMessage;
        for (MaskingRule rule : RULES) {
            result = rule.pattern().matcher(result).replaceAll(rule.replacement());
        }
        return result;
    }
}