package com.aiotb.project.domain.inspections;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.aiotb.project.domain.BaseEntity;
import com.aiotb.project.domain.bridges.Bridge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inspections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inspection extends BaseEntity {

    // id(UUID)는 BaseEntity에서 관리합니다. (중복 선언 시 id 컬럼이 이중 매핑됨)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bridge_no", nullable = false)
    private Bridge bridge; // 점검 대상 교량

    @Column(name = "corp_no", nullable = false)
    private Long corpNo; // 소속 기업 번호

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 점검 기록명

    @Column(name = "inspection_type", nullable = false, length = 50)
    private String inspectionType; // 자체 안전점검, 정기 안전점검, 정밀 안전진단, 긴급 안전점검

    @Column(nullable = false, length = 20)
    private String status; // 진행 대기, 진행 중, 완료

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate; // 점검 날짜

    @Column(name = "inspector_no", nullable = false)
    private Long inspectorNo; // 점검 담당자 번호

    @Column(name = "memo", columnDefinition = "TEXT")
    private String inspectionMemo; // 담당자 메모

    @Column(name = "overall_result", columnDefinition = "TEXT")
    private String overallResult;

    @Column(name = "review_opinion", columnDefinition = "TEXT")
    private String reviewOpinion;

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 점검 완료 일시

    @Column(name = "created_by", nullable = false)
    private Long createdBy; // 기록 생성 사용자 번호

    @Column(name = "updated_by")
    private Long updatedBy; // 최종 수정 사용자 번호

    @Builder
    public Inspection(String id, Bridge bridge, Long corpNo, String title, String inspectionType, String status,
            LocalDate inspectionDate, Long inspectorNo, String inspectionMemo, String overallResult,
            String reviewOpinion, Long createdBy) {
        setId(id); // null이면 BaseEntity @PrePersist에서 UUID 생성
        this.bridge = bridge;
        this.corpNo = corpNo;
        this.title = title;
        this.inspectionType = inspectionType;
        this.status = status != null ? status : "진행 대기";
        this.inspectionDate = inspectionDate;
        this.inspectorNo = inspectorNo;
        this.inspectionMemo = inspectionMemo;
        this.overallResult = overallResult;
        this.reviewOpinion = reviewOpinion;
        this.createdBy = createdBy;
        this.completedAt = "완료".equals(this.status) ? LocalDateTime.now() : null;
    }

    // 점검 정보 수정 메서드
    public void updateInspection(String title, String inspectionType, String status, LocalDate inspectionDate,
            String inspectionMemo, String overallResult, String reviewOpinion) {
        this.title = title;
        this.inspectionType = inspectionType;
        this.status = status;
        this.inspectionDate = inspectionDate;
        this.inspectionMemo = inspectionMemo;
        this.overallResult = overallResult;
        this.reviewOpinion = reviewOpinion;
    }

    /** 수정자 기록과 완료 일시를 함께 갱신하는 수정 메서드 */
    public void updateInspection(String title, String inspectionType, String status, LocalDate inspectionDate,
            String inspectionMemo, String overallResult, String reviewOpinion, Long updatedBy) {
        updateInspection(title, inspectionType, status, inspectionDate, inspectionMemo, overallResult, reviewOpinion);
        this.updatedBy = updatedBy;
        if ("완료".equals(status)) {
            if (this.completedAt == null) this.completedAt = LocalDateTime.now();
        } else {
            this.completedAt = null;
        }
    }
}