package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.AnalysisThreadStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisThreadStatRepository extends JpaRepository<AnalysisThreadStatEntity, Long> {

    List<AnalysisThreadStatEntity> findByLogAnalysisId(Long logAnalysisId);
}