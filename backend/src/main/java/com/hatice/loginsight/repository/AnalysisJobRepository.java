package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import com.hatice.loginsight.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobRepository
        extends JpaRepository<AnalysisJobEntity, UUID>, JpaSpecificationExecutor<AnalysisJobEntity> {

    List<AnalysisJobEntity> findByStatus(JobStatus status);

    Optional<AnalysisJobEntity> findByAnalysisId(Long analysisId);
}