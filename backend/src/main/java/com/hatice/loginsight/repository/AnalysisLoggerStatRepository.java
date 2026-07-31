package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.AnalysisLoggerStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisLoggerStatRepository extends JpaRepository<AnalysisLoggerStatEntity, Long> {

    List<AnalysisLoggerStatEntity> findByLogAnalysisId(Long logAnalysisId);
}