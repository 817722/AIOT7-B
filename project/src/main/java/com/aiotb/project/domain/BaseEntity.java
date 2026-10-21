package com.aiotb.project.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

/**
 * 엔터티의 공통 변수를 정의
 */
@Getter 
@Setter 
@MappedSuperclass          
// 다른 엔터티 클래스들이 상속받아 공통변수를 갖도록 지정
@EntityListeners(AuditingEntityListener.class)
// JPA Auditing 기능 설정 : 생성일, 수정일 등의 정보를 자동으로 기록해주는 기능
public abstract class BaseEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;                      // PK : 기본키, AUTO_INCREMENT
    
    @Column(unique = true, length = 64)
    private String id;                    // UK : 고유키, UUID

    @CreatedDate                          // 생성 시간 기록
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;      // 등록일자 

    @LastModifiedDate                     // 수정 시간 기록
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;      // 수정일자

    @PrePersist                           // INSERT 직전에 호출되는 메소드 지정
    private void generateId() {
        if( this.id == null ) {
            this.id = UUID.randomUUID().toString();
        }
    }
    
}
