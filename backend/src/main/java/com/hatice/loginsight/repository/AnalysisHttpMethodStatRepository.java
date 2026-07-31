package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.AnalysisHttpMethodStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisHttpMethodStatRepository extends JpaRepository<AnalysisHttpMethodStatEntity, Long> {

    List<AnalysisHttpMethodStatEntity> findByLogAnalysisId(Long logAnalysisId);
}