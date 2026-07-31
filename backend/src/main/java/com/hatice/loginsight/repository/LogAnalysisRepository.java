package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.LogAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LogAnalysisRepository
        extends JpaRepository<LogAnalysisEntity, Long>, JpaSpecificationExecutor<LogAnalysisEntity> {

    @Query("SELECT l FROM LogAnalysisEntity l LEFT JOIN FETCH l.frequentErrors WHERE l.id = :id")
    Optional<LogAnalysisEntity> findByIdWithFrequentErrors(@Param("id") Long id);
}