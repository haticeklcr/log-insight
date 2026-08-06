package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.UploadSessionEntity;
import com.hatice.loginsight.entity.UploadSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSessionEntity, UUID> {

    List<UploadSessionEntity> findByStatusIn(List<UploadSessionStatus> statuses);
}