package com.hatice.loginsight.repository;

import com.hatice.loginsight.entity.FrequentErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrequentErrorRepository extends JpaRepository<FrequentErrorEntity, Long> {
}