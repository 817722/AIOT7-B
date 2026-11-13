package com.aiotb.project.controller.bridges;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.service.bridges.BridgeRepresentativeImageService;
import lombok.RequiredArgsConstructor;

/** 승인된 동일 기업 사용자 대상 대표 이미지 전용 API. 교량 전체 삭제 API와 분리. */
@RestController
@RequestMapping("/api/bridge-ui/{bridgeNo}/image")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSPECTOR', 'SUPER_ADMIN')")
public class BridgeRepresentativeImageController {
    private final BridgeRepresentativeImageService images;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(@PathVariable("bridgeNo") long bridgeNo,
                                       @RequestParam("file") MultipartFile file,
                                       Authentication auth) {
        images.upload(bridgeNo, file, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<byte[]> read(@PathVariable("bridgeNo") long bridgeNo, Authentication auth) {
        BridgeRepresentativeImageService.ImageData image = images.read(bridgeNo, auth);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(image.mime()))
                .body(image.bytes());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable("bridgeNo") long bridgeNo, Authentication auth) {
        images.delete(bridgeNo, auth);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
