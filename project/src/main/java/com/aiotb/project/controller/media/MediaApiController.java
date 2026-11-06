package com.aiotb.project.controller.media;

import com.aiotb.project.domain.media.Media;
import com.aiotb.project.service.media.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaApiController {

    private final MediaService mediaService;

    // [GET] 특정 대상의 미디어 목록 조회 API
    @GetMapping("/{parentType}/{parentId}")
    public ResponseEntity<List<Media>> getMediaList(
            @PathVariable("parentType") Media.ParentType parentType,
            @PathVariable("parentId") Long parentId
    ) {
        List<Media> mediaList = mediaService.getMediaList(parentType, parentId);
        return ResponseEntity.ok(mediaList);
    }

    // [DELETE] 미디어 삭제 API (DB 기록 + 실제 파일 함께 삭제)
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<String> deleteMedia(@PathVariable("mediaId") Long mediaId) {
        try {
            // 미디어 정보 조회
            Media media = mediaService.getMediaById(mediaId); // (만약 서비스에 단건 조회가 없다면 아래 참고)

            // 서버 디렉토리에 실제 저장된 파일 삭제
            if (media.getPath() != null) {
                File file = new File(media.getPath());
                if (file.exists()) {
                    file.delete();
                }
            }

            // DB에서 미디어 데이터 삭제
            mediaService.deleteMedia(mediaId);

            return ResponseEntity.ok("미디어가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("미디어 삭제 실패: " + e.getMessage());
        }
    }

    // [POST] 특정 대상(MAINTENANCE 등)에 파일(영상/이미지)을 따로 업로드하는 API
    @PostMapping("/{parentType}/{parentId}")
    public ResponseEntity<String> uploadMedia(
            @PathVariable("parentType") Media.ParentType parentType,
            @PathVariable("parentId") Long parentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("corpNo") Long corpNo,
            @RequestParam("uploadedBy") Long uploadedBy
    ) {
        try {
            // 이미 만들어둔 MediaService의 saveFile 재사용!
            mediaService.saveFile(file, corpNo, parentType, parentId, uploadedBy);
            return ResponseEntity.ok("미디어가 성공적으로 업로드되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("미디어 업로드 실패: " + e.getMessage());
        }
    }
}