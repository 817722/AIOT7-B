package com.aiotb.project.domain.maintenance;

import java.time.LocalDate;

import com.aiotb.project.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 교량 유지보수 기록. 영상과 현장 기록 파일은 media 테이블에서 연결합니다. */
@Entity
@Table(name = "maintenance_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Maintenance extends BaseEntity {
    @Column(name = "bridge_no", nullable = false)
    private Long bridgeNo;
    @Column(name = "corp_no", nullable = false)
    private Long corpNo;
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    @Column(name = "maintenance_type", length = 50)
    private String maintenanceType;
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "진행 대기";
    @Column(name = "maintenance_date")
    private LocalDate maintenanceDate;
    @Column(name = "manager_no", nullable = false)
    private Long managerNo;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "updated_by")
    private Long updatedBy;

    public Maintenance(Long bridgeNo, Long corpNo, String title, String maintenanceType,
            String memo, LocalDate maintenanceDate, Long managerNo, Long createdBy) {
        this.bridgeNo = bridgeNo;
        this.corpNo = corpNo;
        this.title = title;
        this.maintenanceType = maintenanceType;
        this.memo = memo;
        this.maintenanceDate = maintenanceDate;
        this.managerNo = managerNo;
        this.createdBy = createdBy;
    }

    public void update(String title, String type, String memo, LocalDate date, Long managerNo, Long updatedBy) {
        this.title = title;
        this.maintenanceType = type;
        this.memo = memo;
        this.maintenanceDate = date;
        this.managerNo = managerNo;
        this.updatedBy = updatedBy;
    }
}
