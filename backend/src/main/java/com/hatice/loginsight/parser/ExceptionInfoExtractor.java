package com.hatice.loginsight.parser;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExceptionInfoExtractor {

    private static final Pattern INLINE_EXCEPTION_PATTERN =
            Pattern.compile("([\\w$]+(?:\\.[\\w$]+)*Exception)(?::\\s*(.*))?");

    private static final Pattern CAUSED_BY_DETAIL_PATTERN =
            Pattern.compile("^Caused by:\\s*([\\w$]+(?:\\.[\\w$]+)*)(?::\\s*(.*))?$");

    public MultilineExceptionInfo extract(LogRecordGroup group) {
        String[] primary = findInlineException(group.getHeaderLine());

        String rootCauseType = primary != null ? primary[0] : null;
        String rootCauseMessage = primary != null ? primary[1] : null;
        boolean foundAny = primary != null;

        for (String line : group.getContinuationLines()) {
            String trimmed = line.trim();

            Matcher causedByMatcher = CAUSED_BY_DETAIL_PATTERN.matcher(trimmed);
            if (causedByMatcher.matches()) {
                // Zincirdeki HER "Caused by" root cause adayı olur;
                // en son bulunan (dosyada en derinde olan) kazanır.
                rootCauseType = causedByMatcher.group(1);
                rootCauseMessage = causedByMatcher.group(2);
                foundAny = true;
                continue;
            }

            if (primary == null) {
                String[] bare = findInlineException(trimmed);
                if (bare != null) {
                    primary = bare;
                    if (rootCauseType == null) {
                        rootCauseType = bare[0];
                        rootCauseMessage = bare[1];
                    }
                    foundAny = true;
                }
            }
        }

        if (!foundAny) {
            return null;
        }

        String exceptionType = primary != null ? primary[0] : rootCauseType;
        String exceptionMessage = primary != null ? primary[1] : rootCauseMessage;
        boolean multiline = !group.getContinuationLines().isEmpty();

        return new MultilineExceptionInfo(exceptionType, exceptionMessage, rootCauseType, rootCauseMessage, multiline);
    }

   
    private String[] findInlineException(String line) {
        Matcher matcher = INLINE_EXCEPTION_PATTERN.matcher(line);
        if (matcher.find()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        return null;
    }
}