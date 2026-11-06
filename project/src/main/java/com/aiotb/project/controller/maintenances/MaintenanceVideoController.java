package com.aiotb.project.controller.maintenances;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.service.maintenance.MaintenanceVideoService;

import lombok.RequiredArgsConstructor;

/** 회사별 권한 검증된 유지보수 영상 API. 실제 저장파일은 static 경로로 공개하지 않습니다. */
@RestController
@RequestMapping("/api/maintenance-ui/records/{maintenanceNo}/videos")
@RequiredArgsConstructor
public class MaintenanceVideoController {
    private final MaintenanceVideoService service;

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable("maintenanceNo") long no, Authentication auth) {
        return service.list(auth, no);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Long> upload(@PathVariable("maintenanceNo") long no,
            @RequestPart("file") MultipartFile file, Authentication auth) {
        return Map.of("no", service.upload(auth, no, file));
    }

    // ResponseEntity<Resource>는 Spring MVC의 HTTP Range 응답 처리를 사용할 수 있어
    // HTML5 video 태그에서 탐색(seek)을 지원합니다.
    @GetMapping("/{videoNo}/stream")
    public ResponseEntity<Resource> stream(@PathVariable("maintenanceNo") long no,
            @PathVariable("videoNo") long videoNo, Authentication auth) throws MalformedURLException {
        var video = service.video(auth, no, videoNo);
        Resource resource = new UrlResource(video.path().toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(video.mimeType()))
                .body(resource);
    }

    @DeleteMapping("/{videoNo}")
    public ResponseEntity<Void> delete(@PathVariable("maintenanceNo") long no,
            @PathVariable("videoNo") long videoNo, Authentication auth) {
        service.delete(auth, no, videoNo);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> responseError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",
                ex.getReason() == null ? "요청에 실패했습니다." : ex.getReason()));
    }
}
