package com.aiotb.project.controller.media;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.domain.media.Media;
import com.aiotb.project.service.media.MediaService;
import com.aiotb.project.service.users.CurrentUserService;

import lombok.RequiredArgsConstructor;

/**
 * 첨부파일(media) REST API.
 * - 접근: 로그인 + 승인된 사용자 — SecurityConfig / @PreAuthorize
 * - 삭제: ADMIN 전용
 * - 데이터 범위: 로그인 사용자의 소속 기업 — MediaService
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSPECTOR', 'SUPER_ADMIN')")
public class MediaApiController {

    private final MediaService mediaService;
    private final CurrentUserService currentUserService;

    // [GET] 특정 대상의 첨부파일 목록 조회 (서버 저장 경로는 응답하지 않음)
    @GetMapping("/{parentType}/{parentId}")
    public ResponseEntity<List<MediaService.MediaInfo>> getMediaList(
            @PathVariable("parentType") Media.ParentType parentType,
            @PathVariable("parentId") Long parentId) {
        return ResponseEntity.ok(mediaService.getMediaList(parentType, parentId, currentUserService.current()));
    }

    // [GET] 첨부파일 보기/재생 (Range 요청 지원으로 영상 탐색 가능)
    @GetMapping("/file/{mediaNo}")
    public ResponseEntity<Resource> file(@PathVariable("mediaNo") Long mediaNo) throws MalformedURLException {
        MediaService.StoredFile stored = mediaService.getFile(mediaNo, currentUserService.current());
        MediaType type = stored.mimeType() == null
                ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(stored.mimeType());
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(stored.originName(), StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(new UrlResource(stored.path().toUri()));
    }

    // [DELETE] 첨부파일 삭제 (기업 관리자 전용, DB 기록 삭제 후 커밋되면 실제 파일 삭제)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{mediaNo}")
    public ResponseEntity<Map<String, String>> deleteMedia(@PathVariable("mediaNo") Long mediaNo) {
        mediaService.deleteMedia(mediaNo, currentUserService.current());
        return ResponseEntity.ok(Map.of("message", "첨부파일이 삭제되었습니다."));
    }

    // [POST] 점검/유지보수 기록에 파일(영상/이미지) 업로드
    // corpNo, uploadedBy 파라미터는 기존 클라이언트 호환을 위해 받기만 하고 사용하지 않는다(로그인 사용자 기준).
    @PostMapping(value = "/{parentType}/{parentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadMedia(
            @PathVariable("parentType") Media.ParentType parentType,
            @PathVariable("parentId") Long parentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "corpNo", required = false) Long ignoredCorpNo,
            @RequestParam(value = "uploadedBy", required = false) Long ignoredUploadedBy) {
        CurrentUserService.Actor actor = currentUserService.current();
        Long no = mediaService.saveFile(file, actor.corpNo(), parentType, parentId, actor.userNo());
        return ResponseEntity.ok(Map.of("no", no, "message", "첨부파일이 업로드되었습니다."));
    }
}
