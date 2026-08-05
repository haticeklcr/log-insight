package com.hatice.loginsight.parser;

import java.util.regex.Pattern;

public final class ContinuationLineDetector {

    private static final Pattern MORE_PATTERN = Pattern.compile("^\\.\\.\\.\\s*\\d+\\s*more$");
    private static final Pattern CAUSED_BY_PATTERN = Pattern.compile("^Caused by:.*$");
    private static final Pattern SUPPRESSED_PATTERN = Pattern.compile("^Suppressed:.*$");
    private static final Pattern BARE_EXCEPTION_PATTERN =
            Pattern.compile("^[\\w$]+(\\.[\\w$]+)*Exception:?.*$");

    private ContinuationLineDetector() {
    }

    public static boolean isContinuationLine(String rawLine) {
        return isExplicitPattern(rawLine.trim());
    }

    public static boolean isContinuationLine(String rawLine, boolean headerIsErrorRecord) {
        if (isExplicitPattern(rawLine.trim())) {
            return true;
        }
        return headerIsErrorRecord && isIndented(rawLine);
    }

    private static boolean isExplicitPattern(String trimmed) {
        return trimmed.startsWith("at ")
                || MORE_PATTERN.matcher(trimmed).matches()
                || CAUSED_BY_PATTERN.matcher(trimmed).matches()
                || SUPPRESSED_PATTERN.matcher(trimmed).matches()
                || BARE_EXCEPTION_PATTERN.matcher(trimmed).matches();
    }

    private static boolean isIndented(String rawLine) {
        return !rawLine.isEmpty() && Character.isWhitespace(rawLine.charAt(0));
    }
}