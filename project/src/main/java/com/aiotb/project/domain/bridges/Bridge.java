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

@Entity
@Table(name = "bridges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bridge extends BaseEntity {

    @Column(nullable = false, length = 64, unique = true)
    private String id; // 시스템 고유 식별자

    @Column(name = "source_data_date")
    private LocalDate sourceDataDate; // CSV 데이터 기준일자

    @Column(nullable = false, length = 48)
    private String name; // 교량명

    @Column(name = "facility_type", length = 10)
    private String facilityType; // 시설물종별등급구분 (제1종, 제2종, 제3종)

    @Column(name = "road_type", length = 14)
    private String roadType; // 도로종류

    @Column(name = "road_name", length = 21)
    private String roadName; // 도로노선명

    @Column(length = 14)
    private String province; // 시도명

    @Column(length = 17)
    private String city; // 시군구명

    @Column(length = 500)
    private String address; // 소재지지번주소

    @Column(name = "bridge_length", precision = 10, scale = 2)
    private BigDecimal bridgeLength; // 교량연장(m)

    @Column(name = "bridge_width", precision = 8, scale = 2)
    private BigDecimal bridgeWidth; // 교량폭(m)

    @Column(name = "management_agency", length = 35)
    private String managementAgency; // 관리기관명

    @Column(name = "created_by", nullable = false)
    private Long createdBy; // 등록 사용자 번호

    @Builder
    public Bridge(String id, LocalDate sourceDataDate, String name, String facilityType, String roadType,
            String roadName, String province, String city, String address, BigDecimal bridgeLength,
            BigDecimal bridgeWidth, String managementAgency, Long createdBy) {
        this.id = id;
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
        this.createdBy = createdBy;
    }

    // 수정 로직
    public void updateBridge(String name, String facilityType, String address, String province, String city,
            BigDecimal bridgeLength, BigDecimal bridgeWidth, String managementAgency) {
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