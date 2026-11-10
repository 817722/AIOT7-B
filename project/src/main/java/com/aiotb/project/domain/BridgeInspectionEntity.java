package com.aiotb.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table 
@Getter 
@Setter 
public class BridgeInspectionEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 고유 번호 (자동 증가)

    private String keyword;        // 교량명
    private String inspectionType; // 점검 분류
    private String inspector;      // 담당자
    private String status;         // 진행 상태

    @Column(length = 1000)
    private String safetyMemo;     // 안전 메모
    private String risk;           // 위험도
    
}
