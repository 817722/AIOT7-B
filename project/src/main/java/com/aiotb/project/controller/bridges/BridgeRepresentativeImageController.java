package com.aiotb.project.controller.bridges;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.service.bridges.BridgeRepresentativeImageService;
import lombok.RequiredArgsConstructor;

/** 기존 /api/bridge-ui API와 같은 prefix, 서로 다른 상세 경로만 추가. */
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
}
