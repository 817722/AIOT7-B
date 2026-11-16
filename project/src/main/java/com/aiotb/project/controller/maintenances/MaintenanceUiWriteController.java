package com.aiotb.project.controller.maintenances;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.service.maintenance.MaintenanceUiWriteService;
import com.aiotb.project.service.maintenance.MaintenanceUiWriteService.Payload;

import lombok.RequiredArgsConstructor;

/** 등록·수정 전용 API. 팀원의 기존 /api/maintenance와 경로가 겹치지 않습니다. */
@RestController
@RequestMapping("/api/maintenance-ui")
@RequiredArgsConstructor
public class MaintenanceUiWriteController {
    private final MaintenanceUiWriteService service;

    @GetMapping("/bridges")
    public List<Map<String, Object>> bridges(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            Authentication auth) {
        return service.bridges(auth, keyword);
    }

    @GetMapping("/inspections")
    public List<Map<String, Object>> inspections(
            @RequestParam("bridgeNo") long bridgeNo, Authentication auth) {
        return service.inspections(auth, bridgeNo);
    }

    @PostMapping("/records")
    public Map<String, Long> create(@RequestBody Payload request, Authentication auth) {
        return Map.of("no", service.create(auth, request));
    }

    @PutMapping("/records/{no}")
    public Map<String, Long> update(@PathVariable("no") long no,
            @RequestBody Payload request, Authentication auth) {
        service.update(auth, no, request);
        return Map.of("no", no);
    }

    @DeleteMapping("/records/{no}")
    public ResponseEntity<Void> delete(@PathVariable("no") long no, Authentication auth) {
        service.delete(auth, no);
        return ResponseEntity.noContent().build();
    }

    /**
     * JSON 본문을 읽지 못함 (예: "maintenanceDate":"202612-05-01", "2026-02-30") → 개발자용 오류
     * 문구 대신 안내
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", "입력값 형식이 올바르지 않습니다. 날짜는 YYYY-MM-DD 형식(연도 4자리)으로 입력해 주세요."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> responseError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "message", ex.getReason() == null ? "요청을 처리하지 못했습니다." : ex.getReason()));
    }
}
