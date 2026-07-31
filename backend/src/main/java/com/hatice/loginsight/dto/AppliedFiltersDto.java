package com.hatice.loginsight.dto;

import com.hatice.loginsight.entity.AnalysisJobEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class AppliedFiltersDto {

    private Instant startTime;
    private Instant endTime;
    private List<String> levels;
    private String logger;
    private String thread;
    private String messageContains;
    private List<String> statusCodes;
    private List<String> httpMethods;
    private String pathContains;

    public AppliedFiltersDto() {
    }

    public static AppliedFiltersDto from(AnalysisJobEntity job) {
        AppliedFiltersDto dto = new AppliedFiltersDto();
        dto.startTime = job.getFilterStartTime();
        dto.endTime = job.getFilterEndTime();
        dto.levels = csvToList(job.getFilterLevels());
        dto.logger = job.getFilterLogger();
        dto.thread = job.getFilterThread();
        dto.messageContains = job.getFilterMessageContains();
        dto.statusCodes = csvToList(job.getFilterStatusCodes());
        dto.httpMethods = csvToList(job.getFilterHttpMethods());
        dto.pathContains = job.getFilterPathContains();
        return dto;
    }

    private static List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .toList();
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public List<String> getLevels() {
        return levels;
    }

    public void setLevels(List<String> levels) {
        this.levels = levels;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getMessageContains() {
        return messageContains;
    }

    public void setMessageContains(String messageContains) {
        this.messageContains = messageContains;
    }

    public List<String> getStatusCodes() {
        return statusCodes;
    }

    public void setStatusCodes(List<String> statusCodes) {
        this.statusCodes = statusCodes;
    }

    public List<String> getHttpMethods() {
        return httpMethods;
    }

    public void setHttpMethods(List<String> httpMethods) {
        this.httpMethods = httpMethods;
    }

    public String getPathContains() {
        return pathContains;
    }

    public void setPathContains(String pathContains) {
        this.pathContains = pathContains;
    }
}