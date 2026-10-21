package com.aiotb.project.domain.corps;

import java.time.LocalDate;

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
@Table(name = "corps")
public class Corp extends BaseEntity {

    @Column(name = "bis_no", nullable = false, unique = true, length = 20)
    private String bisNo; // 사업자등록번호

    @Column(name = "ceo_name", nullable = false, length = 50)
    private String ceoName; // 대표자명

    @Column(length = 100)
    private String corpName; // 회사명

    @Column(name = "opened_at", nullable = false)
    private LocalDate openDate; // 개업일자 (YYYYMMDD)

    @Column(length = 30)
    private String phone; // 연락처

    @Column(length = 254)
    private String email; // 기업 이메일

    @Builder.Default
    @Column(name = "bis_verified", nullable = false)
    private boolean verified = false; // 기업 인증 여부

}