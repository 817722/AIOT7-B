package com.aiotb.project.controller.inspections;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.service.inspections.InspectionVideoService;
import com.aiotb.project.service.media.MediaService;
import com.aiotb.project.service.media.MediaService.MediaInfo; // 💡 MediaInfo 임포트
import com.aiotb.project.service.users.CurrentUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inspections/videos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSPECTOR', 'SUPER_ADMIN')")
public class InspectionVideoController {

    private final InspectionVideoService inspectionVideoService;
    private final CurrentUserService currentUserService;

    // 특정 점검에 드론 영상 단일/다중 추가 업로드
    @PostMapping("/{inspectionNo}")
    public ResponseEntity<String> uploadVideos(
            @PathVariable("inspectionNo") Long inspectionNo,
            @RequestParam("files") List<MultipartFile> files) {
        inspectionVideoService.saveInspectionVideos(inspectionNo, files, currentUserService.current());
        return ResponseEntity.ok("드론 영상이 성공적으로 업로드되었습니다.");
    }

    // 특정 점검의 영상 목록 조회
    @GetMapping("/{inspectionNo}")
    public ResponseEntity<List<MediaInfo>> getVideosByInspection(@PathVariable("inspectionNo") Long inspectionNo) {
        List<MediaInfo> videos = inspectionVideoService.getVideos(inspectionNo, currentUserService.current());
        return ResponseEntity.ok(videos);
    }
}