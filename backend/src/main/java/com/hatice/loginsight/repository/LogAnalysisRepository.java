package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.LogAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LogAnalysisRepository
        extends JpaRepository<LogAnalysisEntity, Long>, JpaSpecificationExecutor<LogAnalysisEntity> {
}