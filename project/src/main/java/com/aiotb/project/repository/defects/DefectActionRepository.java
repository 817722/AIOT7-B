package com.aiotb.project.repository.defects;

import com.aiotb.project.domain.defects.DefectAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DefectActionRepository extends JpaRepository<DefectAction, Long> {
    
    // 고유 UUID로 조회
    Optional<DefectAction> findById(String id);

    // 특정 결함에 연결된 조치 이력 목록 조회
    List<DefectAction> findByDefectNo(Long defectNo);

    // 처리 상태별 조치 이력 목록 조회
    List<DefectAction> findByStatus(String status);
}