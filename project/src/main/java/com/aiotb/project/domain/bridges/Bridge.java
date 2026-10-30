package com.aiotb.project.domain.bridges;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.aiotb.project.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현재 DDL-ALOHA(1).sql의 bridges 컬럼과 매핑.
 * BaseEntity에 no/id/createdAt/updatedAt이 있으므로 id를 중복 선언하지 않습니다.
 * 원본 데이터 전체의 복사/등록은 BridgeUiWriteService(JDBC) 담당입니다.
 */
@Entity
@Table(name = "bridges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bridge extends BaseEntity {
    @Column(name = "bridge_master_no", nullable = false)
    private Long bridgeMasterNo;

    @Column(name = "corp_no", nullable = false)
    private Long corpNo;

    @Column(name = "source_data_date")
    private LocalDate sourceDataDate;

    @Column(name = "bridge_name", nullable = false, length = 255)
    private String name;

    @Column(name = "facility_type", length = 50)
    private String facilityType;

    @Column(name = "road_type", length = 100)
    private String roadType;

    @Column(name = "road_name", length = 255)
    private String roadName;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "bridge_length", precision = 12, scale = 2)
    private BigDecimal bridgeLength;

    @Column(name = "bridge_width", precision = 10, scale = 2)
    private BigDecimal bridgeWidth;

    @Column(name = "management_agency", length = 255)
    private String managementAgency;

    @Column(name = "manager_no")
    private Long managerNo;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    /** 기존 BridgeService의 Builder 사용 형태를 유지합니다. */
    @Builder
    public Bridge(String id, Long bridgeMasterNo, Long corpNo,
                  LocalDate sourceDataDate, String name, String facilityType,
                  String roadType, String roadName, String province,
                  String city, String address, BigDecimal bridgeLength,
                  BigDecimal bridgeWidth, String managementAgency,
                  Long managerNo, Long createdBy, Long updatedBy) {
        setId(id); // BaseEntity가 관리하는 UUID
        this.bridgeMasterNo = bridgeMasterNo;
        this.corpNo = corpNo;
        this.sourceDataDate = sourceDataDate;
        this.name = name;
        this.facilityType = facilityType;
        this.roadType = roadType;
        this.roadName = roadName;
        this.province = province;
        this.city = city;
        this.address = address;
        this.bridgeLength = bridgeLength;
        this.bridgeWidth = bridgeWidth;
        this.managementAgency = managementAgency;
        this.managerNo = managerNo;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    // 기존 Service의 호출 시그니처를 보존. 신규 UI는 원본 필드를 수정하지 않습니다.
    public void updateBridge(String name, String facilityType, String address,
                             String province, String city,
                             BigDecimal bridgeLength, BigDecimal bridgeWidth,
                             String managementAgency) {
        this.name = name;
        this.facilityType = facilityType;
        this.address = address;
        this.province = province;
        this.city = city;
        this.bridgeLength = bridgeLength;
        this.bridgeWidth = bridgeWidth;
        this.managementAgency = managementAgency;
    }
}
