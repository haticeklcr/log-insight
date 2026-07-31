package com.hatice.loginsight.service;

import com.hatice.loginsight.exception.UnsupportedFilterForParserException;
import com.hatice.loginsight.parser.LogFormat;

import java.util.EnumSet;
import java.util.Set;

public class AnalysisFilterSupport {

    private static final Set<LogFormat> SUPPORTS_LOGGER_AND_THREAD = EnumSet.of(LogFormat.SPRING_BOOT, LogFormat.JSON);
    private static final Set<LogFormat> SUPPORTS_HTTP_FIELDS =
            EnumSet.of(LogFormat.NGINX_ACCESS, LogFormat.APACHE_ACCESS);

    private AnalysisFilterSupport() {
    }

    public static void validate(LogFormat format, boolean loggerSet, boolean threadSet, boolean statusCodesSet,
                                 boolean httpMethodsSet, boolean pathContainsSet) {
        if ((loggerSet || threadSet) && !SUPPORTS_LOGGER_AND_THREAD.contains(format)) {
            throw new UnsupportedFilterForParserException(
                    "logger/thread filtresi " + format + " parseri icin desteklenmiyor");
        }
        if ((statusCodesSet || httpMethodsSet || pathContainsSet) && !SUPPORTS_HTTP_FIELDS.contains(format)) {
            throw new UnsupportedFilterForParserException(
                    "statusCodes/httpMethods/pathContains filtresi " + format + " parseri icin desteklenmiyor");
        }
    }
}