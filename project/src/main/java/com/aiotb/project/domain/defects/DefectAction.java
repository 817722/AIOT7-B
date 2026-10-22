package com.aiotb.project.domain.defects;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.aiotb.project.domain.BaseEntity;

@Entity
@Table(name = "defect_actions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DefectAction extends BaseEntity {

    @Column(nullable = false, length = 64, unique = true)
    private String id; // 시스템 고유 식별자

    @Column(name = "defect_no", nullable = false)
    private Long defectNo; // 대상 결함 번호

    @Column(name = "maintenance_no")
    private Long maintenanceNo; // 연결된 유지보수 기록 번호 (선택)

    @Column(nullable = false, length = 20)
    private String status; // 결함 처리 상태 (발견, 조치 필요, 조치 중, 조치 완료)

    @Column(name = "action_date")
    private LocalDate actionDate; // 조치일

    @Column(name = "manager_no", nullable = false)
    private Long managerNo; // 조치 담당자 번호

    @Column(name = "action_description", columnDefinition = "TEXT")
    private String actionDescription; // 조치 내용

    @Column(columnDefinition = "TEXT")
    private String result; // 조치 결과

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 조치 완료 일시

    @Column(name = "created_by", nullable = false)
    private Long createdBy; // 이력 등록 사용자 번호

    @Builder
    public DefectAction(String id, Long defectNo, Long maintenanceNo, String status, LocalDate actionDate, Long managerNo, String actionDescription, String result, LocalDateTime completedAt, Long createdBy) {
        this.id = id;
        this.defectNo = defectNo;
        this.maintenanceNo = maintenanceNo;
        this.status = status != null ? status : "발견";
        this.actionDate = actionDate;
        this.managerNo = managerNo;
        this.actionDescription = actionDescription;
        this.result = result;
        this.completedAt = completedAt;
        this.createdBy = createdBy;
    }

    // 조치 이력 수정 메서드
    public void updateAction(String status, LocalDate actionDate, Long managerNo, String actionDescription, String result, LocalDateTime completedAt) {
        this.status = status;
        this.actionDate = actionDate;
        this.managerNo = managerNo;
        this.actionDescription = actionDescription;
        this.result = result;
        this.completedAt = completedAt;
    }
}