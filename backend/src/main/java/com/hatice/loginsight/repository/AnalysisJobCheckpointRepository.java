package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.AnalysisJobCheckpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalysisJobCheckpointRepository extends JpaRepository<AnalysisJobCheckpointEntity, UUID> {
}