package com.aiotb.project.repository.defects;

import com.aiotb.project.domain.defects.Defect;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DefectRepository extends JpaRepository<Defect, Long> {
    
    // 고유 UUID로 조회
    Optional<Defect> findById(String id);

    // 특정 점검 기록에서 발견된 결함 목록 조회
    List<Defect> findByInspectionNo(Long inspectionNo);

    // 심각도별 결함 목록 조회
    List<Defect> findBySeverity(String severity);
}