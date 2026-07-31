package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.parser.ParsedLogEntry;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JobFilterCriteria {

    private final Instant startTime;
    private final Instant endTime;
    private final Set<String> levels;
    private final String loggerContains;
    private final String threadContains;
    private final String messageContains;
    private final Set<Integer> statusCodes;
    private final Set<String> httpMethods;
    private final String pathContains;

    private JobFilterCriteria(Instant startTime, Instant endTime, Set<String> levels, String loggerContains,
                               String threadContains, String messageContains, Set<Integer> statusCodes,
                               Set<String> httpMethods, String pathContains) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.levels = levels;
        this.loggerContains = loggerContains;
        this.threadContains = threadContains;
        this.messageContains = messageContains;
        this.statusCodes = statusCodes;
        this.httpMethods = httpMethods;
        this.pathContains = pathContains;
    }

    public static JobFilterCriteria from(AnalysisJobEntity job) {
        return new JobFilterCriteria(
                job.getFilterStartTime(),
                job.getFilterEndTime(),
                toStringSet(job.getFilterLevels()),
                blankToNull(job.getFilterLogger()),
                blankToNull(job.getFilterThread()),
                blankToNull(job.getFilterMessageContains()),
                toIntegerSet(job.getFilterStatusCodes()),
                toStringSet(job.getFilterHttpMethods()),
                blankToNull(job.getFilterPathContains()));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Set<String> toStringSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        Set<String> result = new HashSet<>();
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(String::toUpperCase)
                .forEach(result::add);
        return result;
    }

    private static Set<Integer> toIntegerSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        Set<Integer> result = new HashSet<>();
        for (String token : csv.split(",")) {
            try {
                result.add(Integer.parseInt(token.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public boolean matches(ParsedLogEntry entry) {
        if (startTime != null && (entry.getTimestamp() == null || entry.getTimestamp().isBefore(startTime))) {
            return false;
        }
        if (endTime != null && (entry.getTimestamp() == null || entry.getTimestamp().isAfter(endTime))) {
            return false;
        }
        if (levels != null && (entry.getLevel() == null || !levels.contains(entry.getLevel()))) {
            return false;
        }
        if (loggerContains != null
                && (entry.getLogger() == null || !containsIgnoreCase(entry.getLogger(), loggerContains))) {
            return false;
        }
        if (threadContains != null
                && (entry.getThread() == null || !containsIgnoreCase(entry.getThread(), threadContains))) {
            return false;
        }
        if (messageContains != null
                && (entry.getMessage() == null || !containsIgnoreCase(entry.getMessage(), messageContains))) {
            return false;
        }
        if (statusCodes != null && (entry.getStatusCode() == null || !statusCodes.contains(entry.getStatusCode()))) {
            return false;
        }
        if (httpMethods != null
                && (entry.getMethod() == null || !httpMethods.contains(entry.getMethod().toUpperCase()))) {
            return false;
        }
        if (pathContains != null
                && (entry.getPath() == null || !containsIgnoreCase(entry.getPath(), pathContains))) {
            return false;
        }
        return true;
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }
}