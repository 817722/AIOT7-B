package com.aiotb.project.controller.common;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
 * - 입력 형식 오류(날짜 형식, 필수값 누락 등) → 400 + 사용자용 안내 문구
 * (프레임워크 기본 메시지 "Validation failed…", "JSON parse error…" 를 그대로 노출하지 않음)
 * 인증/권한 예외(401/403)는 Spring Security가 처리하도록 여기서 잡지 않는다.
 */
@RestControllerAdvice(assignableTypes = {
        InspectionController.class, MaintenanceController.class, MediaApiController.class })
public class ApiExceptionAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    /** 사용자에게 안내할 날짜 항목 이름 (DTO 필드명 → 화면 표시명) */
    private static final Map<String, String> DATE_LABELS = Map.of(
            "inspectionDate", "점검 일자",
            "maintenanceDate", "유지보수 진행 날짜");

    static final String DATE_FORMAT_HINT = "날짜는 YYYY-MM-DD 형식(연도 4자리)으로 입력해 주세요.";

    /** multipart/form 바인딩 실패 (예: 점검 등록의 inspectionDate=202612-05-01) */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> bindError(BindException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", bindingMessage(ex)));
    }

    /** @RequestBody @Valid 검증 실패 (필수값 누락 등) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validationError(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", bindingMessage(ex.getBindingResult())));
    }

    /** JSON 본문을 읽지 못함 (예: "maintenanceDate":"202612-05-01", "2026-02-30") */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "입력값 형식이 올바르지 않습니다. " + DATE_FORMAT_HINT));
    }

    private static String bindingMessage(BindingResult result) {
        FieldError error = result.getFieldError();
        if (error == null)
            return "입력값을 확인해 주세요.";
        if (error.isBindingFailure()) {
            // 형식 변환 실패: 날짜 항목이면 날짜 형식 안내, 그 외에는 일반 안내
            String label = DATE_LABELS.get(error.getField());
            return label != null ? label + "를 올바르게 입력해 주세요. " + DATE_FORMAT_HINT : "입력값 형식이 올바르지 않습니다.";
        }
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "입력값을 확인해 주세요.";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",
                ex.getReason() == null ? HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase()
                        : ex.getReason()));
    }
}
