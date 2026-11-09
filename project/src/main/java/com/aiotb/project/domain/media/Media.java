package com.aiotb.project.domain.media;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Media {
    // 실제 media 테이블에는 updated_at 컬럼이 없으므로 BaseEntity(updated_at 포함)를 상속하지 않는다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @Column(name = "id", nullable = false, unique = true, length = 64)
    private String id;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt; // DB 기본값(CURRENT_TIMESTAMP) 사용

    @Column(name = "corp_no")
    private Long corpNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "p_type")
    private ParentType parentType;

    @Column(name = "p_id")
    private Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MediaType type;

    @Column(name = "is_main", nullable = false)
    private boolean main;

    @Column(name = "path", nullable = false, length = 1000)
    private String path;

    @Column(name = "origin_name", nullable = false)
    private String originName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @PrePersist
    private void generateId() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    public enum ParentType {
        BRIDGE, INSPECTION, MAINTENANCE
    }

    public enum MediaType {
        IMAGE, VIDEO
    }

    public Media(Long corpNo, ParentType parentType, Long parentId, MediaType type,
            boolean main, String path, String originName, String mimeType,
            Long fileSize, Long uploadedBy) {
        this.corpNo = corpNo;
        this.parentType = parentType;
        this.parentId = parentId;
        this.type = type;
        this.main = main;
        this.path = path;
        this.originName = originName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
    }

    // 교량 대표 이미지 지정 시 기존 대표 이미지를 먼저 해제한 뒤 호출
    public void setMain(boolean main) {
        this.main = main;
    }

    public void replaceFile(String path, String originName, String mimeType, Long fileSize) {
        this.path = path;
        this.originName = originName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
    }
}
