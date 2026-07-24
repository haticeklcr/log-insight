package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.AnalysisJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AnalysisJobRepository
        extends JpaRepository<AnalysisJobEntity, UUID>, JpaSpecificationExecutor<AnalysisJobEntity> {
}