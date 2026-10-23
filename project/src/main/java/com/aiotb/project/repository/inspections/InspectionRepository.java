package com.aiotb.project.repository.inspections;

import com.aiotb.project.domain.inspections.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    
    // 고유 UUID로 조회
    Optional<Inspection> findById(String id);

    // 특정 교량에 속한 점검 목록 조회
    List<Inspection> findByBridgeNo(Long bridgeNo);

    // 특정 담당자의 점검 목록 조회
    List<Inspection> findByInspectorNo(Long inspectorNo);
}