package com.aiotb.project.domain.media;

import com.aiotb.project.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Media extends BaseEntity {
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
