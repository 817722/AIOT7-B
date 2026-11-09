package com.aiotb.project.domain.corps;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 실제 DB corps 테이블과 매핑.
 * - bis_verified: 국세청 사업자 검증 결과 (검증 성공이 곧 서비스 승인은 아님)
 * - status: SUPER_ADMIN의 가입 승인/반려/정지 상태 (PENDING/ACTIVE/REJECTED/SUSPENDED)
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "corps")
public class Corp {

    /** 기업 서비스 이용 상태 (SUPER_ADMIN이 변경) */
    public enum Status { PENDING, ACTIVE, REJECTED, SUSPENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @Column(name = "bis_no", nullable = false, unique = true, length = 20)
    private String bisNo;

    @Column(name = "ceo_name", nullable = false, length = 100)
    private String ceoName;

    @Column(name = "corp_name", nullable = false, length = 100)
    private String corpName;

    @Column(name = "opened_at", nullable = false)
    private LocalDate openDate;

    @Builder.Default
    @Column(name = "bis_verified", nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "status_by")
    private Long statusBy;           // 처리한 SUPER_ADMIN 사용자 번호

    @Column(name = "status_at")
    private LocalDateTime statusAt;  // 처리 일시

    @Column(name = "reject", length = 500)
    private String reject;           // 반려·정지 사유

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
