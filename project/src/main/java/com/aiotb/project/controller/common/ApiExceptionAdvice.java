package com.aiotb.project.controller.common;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.controller.inspections.InspectionController;
import com.aiotb.project.controller.maintenances.MaintenanceController;
import com.aiotb.project.controller.media.MediaApiController;

/**
 * 기존 REST API(/api/inspections, /api/maintenance, /api/media)의 오류를 JSON으로 응답한다.
 * - 잘못된 입력(IllegalArgumentException) → 400 (기존에는 500으로 응답됨)
 * - ResponseStatusException → 지정된 상태 코드
 * 인증/권한 예외(401/403)는 Spring Security가 처리하도록 여기서 잡지 않는다.
 */
@RestControllerAdvice(assignableTypes = {
        InspectionController.class, MaintenanceController.class, MediaApiController.class })
public class ApiExceptionAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",
                ex.getReason() == null ? HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase() : ex.getReason()));
    }
}
