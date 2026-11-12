package com.aiotb.project.repository;

import com.aiotb.project.domain.BridgeInspectionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BridgeInspectionRepository extends JpaRepository<BridgeInspectionEntity, Long> {
    long countByStatus(String status);
    Page<BridgeInspectionEntity> findByKeywordContainingAndStatus(String keyword, String status, Pageable pageable);
    Page<BridgeInspectionEntity> findByKeywordContaining(String keyword, Pageable pageable);
}