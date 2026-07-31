package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.AnalysisTimelineStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisTimelineStatRepository extends JpaRepository<AnalysisTimelineStatEntity, Long> {

    List<AnalysisTimelineStatEntity> findByLogAnalysisIdOrderByBucketStartAsc(Long logAnalysisId);
}