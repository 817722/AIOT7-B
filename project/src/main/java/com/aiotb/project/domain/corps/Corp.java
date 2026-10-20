package com.aiotb.project.domain.corps;

import com.aiotb.project.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 기업 엔터티
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "corp")
public class Corp extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String businessNo; // 사업자등록번호

    @Column(nullable = false, length = 50)
    private String representativeName; // 대표자명

    @Column(nullable = false, length = 8)
    private String openDate; // 개업일자 (YYYYMMDD)

    @Column(length = 100)
    private String corpNickname; // 회사 별명

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false; // 기업 인증 여부

}