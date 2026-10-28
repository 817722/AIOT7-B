package com.aiotb.project.controller.corps;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiotb.project.dto.corps.VerifiedBusiness;
import com.aiotb.project.repository.corps.CorpRepository;
import com.aiotb.project.service.corps.BusinessVerifyService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessVerificationController {
    private final BusinessVerifyService verificationService;
    private final CorpRepository corpRepository;

    public record VerifyRequest(String businessNo, String openDate,
            String representativeName, String corpName) {
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody VerifyRequest request, HttpSession session) {
        session.removeAttribute(VerifiedBusiness.SESSION_KEY);
        if (request == null) {
            return result(HttpStatus.BAD_REQUEST, false, "기업 정보를 입력해 주세요.");
        }
        String businessNo = request.businessNo() == null ? "" : request.businessNo().replaceAll("\\D", "");
        String ceo = request.representativeName() == null ? "" : request.representativeName().trim();
        String company = request.corpName() == null ? "" : request.corpName().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(request.openDate());
        } catch (DateTimeParseException | NullPointerException e) {
            return result(HttpStatus.BAD_REQUEST, false, "개업일자를 올바르게 입력해 주세요.");
        }
        if (!businessNo.matches("\\d{10}") || ceo.isBlank() || ceo.length() > 100
                || company.isBlank() || company.length() > 100 || date.isAfter(LocalDate.now())) {
            return result(HttpStatus.BAD_REQUEST, false, "사업자번호·대표자명·기업명·개업일자를 확인해 주세요.");
        }
        if (corpRepository.findByBisNo(businessNo).isPresent()) {
            return result(HttpStatus.CONFLICT, false,
                    "이미 등록된 기업입니다. 기업 관리자에게 작업자 계정 등록을 요청해 주세요.");
        }
        try {
            Map<?, ?> response = verificationService.verify(businessNo, date.toString(), ceo);
            Object data = response == null ? null : response.get("data");
            if (!"OK".equals(String.valueOf(response == null ? null : response.get("status_code")))
                    || !(data instanceof List<?> list)) {
                return result(HttpStatus.BAD_GATEWAY, false, "국세청 인증 응답을 확인할 수 없습니다.");
            }
            boolean matched = list.stream()
                    .filter(Map.class::isInstance).map(Map.class::cast)
                    .anyMatch(entry -> businessNo.equals(String.valueOf(entry.get("b_no")).replaceAll("\\D", ""))
                            && "01".equals(String.valueOf(entry.get("valid"))));
            if (!matched) {
                return result(HttpStatus.OK, false, "입력한 사업자 정보가 일치하지 않습니다.");
            }
            session.setAttribute(VerifiedBusiness.SESSION_KEY,
                    new VerifiedBusiness(businessNo, ceo, company, date, Instant.now()));
            return result(HttpStatus.OK, true, "기업 정보가 확인됐습니다. 작업자 정보를 입력해 주세요.");
        } catch (Exception e) {
            log.warn("외부 사업자 진위확인 API 호출 실패", e);
            return result(HttpStatus.BAD_GATEWAY, false, "국세청 인증 API에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private ResponseEntity<Map<String, Object>> result(HttpStatus status, boolean verified, String message) {
        return ResponseEntity.status(status).body(Map.of("verified", verified, "message", message));
    }
}
