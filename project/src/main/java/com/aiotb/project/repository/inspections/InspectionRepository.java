package com.aiotb.project.repository.inspections;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aiotb.project.domain.inspections.Inspection;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    // 1. 고유 ID로 점검 내역 조회 (findByUuid -> findById)
    Optional<Inspection> findById(String id);

    // 2. 특정 교량 번호에 속한 점검 목록 조회
    List<Inspection> findByBridgeNo(Long bridgeNo);

    // 3. 특정 담당자 번호의 점검 목록 조회
    List<Inspection> findByInspectorNo(Long inspectorNo);

    // 4. 기업별 접근 제한용 (ID와 corp_no 기준 조회) (findByUuidAndCorpNo -> findByIdAndCorpNo)
    Optional<Inspection> findByIdAndCorpNo(String id, Long corpNo);

    // 5. 특정 교량의 점검 목록을 날짜 최신순으로 조회
    List<Inspection> findByBridgeNoAndCorpNoOrderByInspectionDateDesc(Long bridgeNo, Long corpNo);

    // 6. 전체 점검 조회
    List<Inspection> findByCorpNoOrderByInspectionDateDesc(Long corpNo);
}