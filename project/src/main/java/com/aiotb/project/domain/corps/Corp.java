package com.aiotb.project.domain.corps;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DDL-ALOHA.sql의 corps 테이블은 id(UUID) 컬럼이 없으므로 BaseEntity를 상속하지 않는다. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "corps")
public class Corp {
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

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
