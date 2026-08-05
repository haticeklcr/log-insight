package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.UploadSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSessionEntity, UUID> {
}