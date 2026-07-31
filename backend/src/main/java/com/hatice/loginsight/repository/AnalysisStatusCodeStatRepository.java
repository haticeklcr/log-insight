package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.AnalysisStatusCodeStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisStatusCodeStatRepository extends JpaRepository<AnalysisStatusCodeStatEntity, Long> {

    List<AnalysisStatusCodeStatEntity> findByLogAnalysisId(Long logAnalysisId);
}