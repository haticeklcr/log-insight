package com.hatice.loginsight.service;

import com.hatice.loginsight.exception.UnsupportedFilterForParserException;
import com.hatice.loginsight.parser.LogFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisFilterSupportTest {

    @Test
    void allowsLoggerFilterForSpringBootFormat() {
        assertDoesNotThrow(() -> AnalysisFilterSupport.validate(LogFormat.SPRING_BOOT, true, false, false, false, false));
    }

    @Test
    void rejectsLoggerFilterForNginxFormat() {
        assertThrows(UnsupportedFilterForParserException.class,
                () -> AnalysisFilterSupport.validate(LogFormat.NGINX_ACCESS, true, false, false, false, false));
    }

    @Test
    void allowsStatusCodeFilterForApacheFormat() {
        assertDoesNotThrow(() -> AnalysisFilterSupport.validate(LogFormat.APACHE_ACCESS, false, false, true, false, false));
    }

    @Test
    void rejectsStatusCodeFilterForJsonFormat() {
        assertThrows(UnsupportedFilterForParserException.class,
                () -> AnalysisFilterSupport.validate(LogFormat.JSON, false, false, true, false, false));
    }

    @Test
    void rejectsHttpMethodFilterForPlainTextFormat() {
        assertThrows(UnsupportedFilterForParserException.class,
                () -> AnalysisFilterSupport.validate(LogFormat.PLAIN_TEXT, false, false, false, true, false));
    }

    @Test
    void allowsNoFiltersForAnyFormat() {
        for (LogFormat format : LogFormat.values()) {
            assertDoesNotThrow(() -> AnalysisFilterSupport.validate(format, false, false, false, false, false));
        }
    }
}