package com.aiotb.project.controller.inspections;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.dto.inspections.InspectionDto;
import com.aiotb.project.service.inspections.InspectionService;
import com.aiotb.project.service.users.CurrentUserService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper; // JSON 문자열 변환용

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 점검 기록 REST API.
 * - 접근: 로그인 + 승인된 사용자(ADMIN / INSPECTOR / SUPER_ADMIN) — SecurityConfig
 * - 삭제: ADMIN 전용 — SecurityConfig + @PreAuthorize
 * - 데이터 범위: 로그인 사용자의 소속 기업 — InspectionService
 */
@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSPECTOR', 'SUPER_ADMIN')")
public class InspectionController {

    private final InspectionService inspectionService;
    private final CurrentUserService currentUserService;

    // 점검 기록 및 드론 파일 등록 API (Multipart 형태의 DTO 직접 바인딩)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createInspection(
            @ModelAttribute @Valid InspectionDto.Request request) {

        // 통합된 서비스 호출 (request 객체 안에 단일/다중 파일과 텍스트 데이터가 모두 들어있음)
        String inspectionId = inspectionService.createInspection(request, currentUserService.current());
        return ResponseEntity.ok(inspectionId);
    }

    // 특정 교량별 점검 목록 조회 API
    @GetMapping("/bridge/{bridgeNo}")
    public ResponseEntity<List<InspectionDto.Response>> getInspectionsByBridge(@PathVariable("bridgeNo") Long bridgeNo) {
        return ResponseEntity.ok(inspectionService.getInspectionsByBridge(bridgeNo, currentUserService.current()));
    }

    // 점검 상세 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<InspectionDto.Response> getInspection(@PathVariable("id") String id) {
        return ResponseEntity.ok(inspectionService.getInspectionById(id, currentUserService.current()));
    }

    // 점검 기록 수정 API
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateInspection(@PathVariable("id") String id,
            @RequestBody @Valid InspectionDto.Request request) {
        inspectionService.updateInspection(id, request, currentUserService.current());
        return ResponseEntity.ok().build();
    }

    // 점검 기록 삭제 API (기업 관리자 전용)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInspection(@PathVariable("id") String id) {
        inspectionService.deleteInspection(id, currentUserService.current());
        return ResponseEntity.ok().build();
    }
}
