package com.aiotb.project.controller.inspections;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.domain.media.Media;
import com.aiotb.project.service.media.MediaService;
import com.aiotb.project.service.media.MediaService.MediaInfo;
import com.aiotb.project.service.users.CurrentUserService;

import lombok.RequiredArgsConstructor;

/**
 * 점검 현장 사진 API (/api/inspections/photos/{inspectionNo}).
 * - 업로드·조회 모두 사진(IMAGE)만 다룬다. 영상은 /api/inspections/videos 에서 관리한다.
 * - 사진 여부는 실제 파일 헤더로 판별 (확장자만 바꾼 영상 파일도 거부)
 * - 소속 기업의 점검 기록인지는 MediaService 에서 확인 (다른 기업 기록은 404)
 */
@RestController
@RequestMapping("/api/inspections/photos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSPECTOR', 'SUPER_ADMIN')")
public class InspectionPhotoController {

    private final MediaService mediaService;
    private final CurrentUserService currentUserService;

    // 특정 점검(Inspection PK 번호 기준)에 첨부된 사진 목록 조회 (영상 제외)
    @GetMapping("/{inspectionNo}")
    public ResponseEntity<List<MediaInfo>> getPhotosByInspection(@PathVariable("inspectionNo") Long inspectionNo) {
        var actor = currentUserService.current();
        List<MediaInfo> photos = mediaService.getMediaList(
                Media.ParentType.INSPECTION, inspectionNo, actor, Media.MediaType.IMAGE);
        return ResponseEntity.ok(photos);
    }

    // 특정 점검에 현장 사진 1장 업로드 (JPG·PNG·WebP 만 허용)
    @PostMapping(value = "/{inspectionNo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadPhoto(@PathVariable("inspectionNo") Long inspectionNo,
            @RequestParam("file") MultipartFile file) {
        var actor = currentUserService.current();
        Long no = mediaService.saveFile(file, actor.corpNo(), Media.ParentType.INSPECTION, inspectionNo,
                actor.userNo(), Media.MediaType.IMAGE);
        return ResponseEntity.ok(Map.of("no", no, "message", "현장 사진이 등록되었습니다."));
    }
}
