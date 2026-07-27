package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import org.springframework.data.jpa.domain.Specification;

public final class AnalysisJobSpecifications {

    private AnalysisJobSpecifications() {
    }

    public static Specification<AnalysisJobEntity> hasAnalysisNameContaining(String analysisName) {
        if (analysisName == null || analysisName.isBlank()) {
            return null;
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("analysisName")), "%" + analysisName.toLowerCase() + "%");
    }

    public static Specification<AnalysisJobEntity> hasFileNameContaining(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("fileName")), "%" + fileName.toLowerCase() + "%");
    }

    public static Specification<AnalysisJobEntity> hasStatus(JobStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}