package com.aiotb.project.controller.bridges;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/**
 * 교량 대표 이미지 API (/api/bridge-ui/{bridgeNo}/image).
 * - 조회·등록·변경·삭제: 같은 기업의 승인된 사용자 모두 가능 (권한 종류 무관)
 * - 기업 소유 확인은 BridgeRepresentativeImageService 에서 수행 (다른 기업 교량은 404)
 * - 교량 자체 삭제(DELETE /api/bridge-ui/{no})는 BridgeUiWriteController 에서 ADMIN 전용으로 유지
 */
@RestController
@RequestMapping("/api/bridge-ui/{bridgeNo}/image")
@RequiredArgsConstructor
public class BridgeRepresentativeImageController {
    private final BridgeRepresentativeImageService images;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(@PathVariable("bridgeNo") long bridgeNo,
                                       @RequestParam("file") MultipartFile file,
                                       Authentication authentication) {
        images.upload(bridgeNo, file, authentication);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    public ResponseEntity<byte[]> read(@PathVariable("bridgeNo") long bridgeNo, Authentication authentication) {
        BridgeRepresentativeImageService.ImageData image = images.read(bridgeNo, authentication);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(image.mime()))
                .body(image.bytes());
    }

    /** 대표 이미지만 삭제 (교량은 삭제하지 않음). CSRF 토큰 필요. */
    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable("bridgeNo") long bridgeNo, Authentication authentication) {
        images.delete(bridgeNo, authentication);
        return ResponseEntity.noContent().build();
    }
}
