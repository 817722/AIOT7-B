package com.aiotb.project.controller.maintenances;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.domain.media.Media;
import com.aiotb.project.service.media.MediaService;
import com.aiotb.project.service.users.CurrentUserService;

import lombok.RequiredArgsConstructor;

/** 유지보수 증거 사진 전용 API. 기존 영상 API 및 공통 미디어 API와 분리됩니다. */
@RestController
@RequestMapping("/api/maintenance-ui/records/{maintenanceNo}/photos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSPECTOR', 'SUPER_ADMIN')")
public class MaintenancePhotoController {
    private final MediaService mediaService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<MediaService.MediaInfo> list(@PathVariable("maintenanceNo") Long maintenanceNo,
                                              Authentication authentication) {
        var actor = currentUserService.current(authentication);
        return mediaService.getMediaList(Media.ParentType.MAINTENANCE, maintenanceNo, actor)
                .stream().filter(item -> "IMAGE".equals(item.type())).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Long> upload(@PathVariable("maintenanceNo") Long maintenanceNo,
                                    @RequestPart("file") MultipartFile file,
                                    Authentication authentication) {
        var actor = currentUserService.current(authentication);
        long no = mediaService.saveMaintenancePhoto(file, actor, maintenanceNo);
        return Map.of("no", no);
    }

    @DeleteMapping("/{photoNo}")
    public ResponseEntity<Void> delete(@PathVariable("maintenanceNo") Long maintenanceNo,
                                       @PathVariable("photoNo") Long photoNo,
                                       Authentication authentication) {
        var actor = currentUserService.current(authentication);
        mediaService.deleteMaintenancePhoto(maintenanceNo, photoNo, actor);
        return ResponseEntity.noContent().build();
    }
}
