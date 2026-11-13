package com.aiotb.project.controller.inspections;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiotb.project.domain.media.Media; // 👈 Media 엔티티/enum 임포트 추가
import com.aiotb.project.service.media.MediaService;
import com.aiotb.project.service.media.MediaService.MediaInfo;
import com.aiotb.project.service.users.CurrentUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inspections/photos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSPECTOR', 'SUPER_ADMIN')")
public class InspectionPhotoController {

    private final MediaService mediaService;
    private final CurrentUserService currentUserService;

    // 특정 점검(Inspection PK 번호 기준)에 첨부된 사진 목록 조회
    @GetMapping("/{inspectionNo}")
    public ResponseEntity<List<MediaInfo>> getPhotosByInspection(@PathVariable("inspectionNo") Long inspectionNo) {
        var actor = currentUserService.current();
        // 💡 MediaService.ParentType -> Media.ParentType 로 변경
        List<MediaInfo> photos = mediaService.getMediaList(Media.ParentType.INSPECTION, inspectionNo, actor);
        return ResponseEntity.ok(photos);
    }
}