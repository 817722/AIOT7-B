package com.aiotb.project.domain.defects;

import com.aiotb.project.domain.BaseEntity;
import com.aiotb.project.domain.inspections.Inspection;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "defects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Defect extends BaseEntity {

    @Column(nullable = false, length = 64, unique = true)
    private String id; // 시스템 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_no", nullable = false)
    private Inspection inspection; // 결함이 발견된 점검 기록

    @Column(name = "defect_type", nullable = false, length = 100)
    private String defectType; // 결함 유형

    @Column(nullable = false, length = 500)
    private String location; // 교량 내 결함 위치

    @Column(length = 20)
    private String severity; // 결함 심각도 (낮음, 보통, 높음, 긴급)

    @Column(columnDefinition = "TEXT")
    private String description; // 결함 상세 설명

    @Column(name = "created_by", nullable = false)
    private Long createdBy; // 등록 사용자 번호

    @Builder
    public Defect(String id, Inspection inspection, String defectType, String location, String severity, String description, Long createdBy) {
        this.id = id;
        this.inspection = inspection;
        this.defectType = defectType;
        this.location = location;
        this.severity = severity;
        this.description = description;
        this.createdBy = createdBy;
    }

    // 결함 정보 수정 메서드
    public void updateDefect(String defectType, String location, String severity, String description) {
        this.defectType = defectType;
        this.location = location;
        this.severity = severity;
        this.description = description;
    }
}