package com.aiotb.project.service.corps;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.domain.corps.Corp;
import com.aiotb.project.dto.corps.CorpDto;
import com.aiotb.project.repository.corps.CorpRepository;
import com.aiotb.project.service.users.BusinessVerificationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorpServiceImpl implements CorpService {
    private final CorpRepository corpRepository;
    private final BusinessVerificationService verificationService;

    @Override
    @Transactional
    public Corp verifyAndRegister(CorpDto dto) {
        String bisNo = dto.getBisNo().replaceAll("\\D", "");
        if (!bisNo.matches("\\d{10}") || dto.getOpenDate() == null ||
                dto.getCeoName() == null || dto.getCeoName().isBlank() ||
                dto.getCorpName() == null || dto.getCorpName().isBlank()) {
            throw new IllegalArgumentException("기업 정보를 올바르게 입력해 주세요.");
        }
        if (corpRepository.findByBisNo(bisNo).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 기업입니다. 기업 관리자에게 계정 등록을 요청해 주세요.");
        }

        // 사용자가 보낸 값이나 브라우저 인증 상태를 신뢰하지 않고 최종 가입 시 국세청에 다시 검증한다.
        Map<?, ?> response = verificationService.verify(bisNo, dto.getOpenDate().toString(), dto.getCeoName());
        Object data = response == null ? null : response.get("data");
        if (!"OK".equals(String.valueOf(response == null ? null : response.get("status_code")))
                || !(data instanceof List<?> dataList)) {
            throw new IllegalArgumentException("국세청 기업 인증 응답을 확인할 수 없습니다.");
        }
        boolean matched = dataList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(item -> bisNo.equals(String.valueOf(item.get("b_no")).replaceAll("\\D", ""))
                        && "01".equals(String.valueOf(item.get("valid"))));
        if (!matched) {
            throw new IllegalArgumentException("사업자 정보가 일치하지 않아 가입할 수 없습니다.");
        }

        Corp corp = Corp.builder()
                .bisNo(bisNo)
                .ceoName(dto.getCeoName().trim())
                .corpName(dto.getCorpName().trim())
                .openDate(dto.getOpenDate())
                .verified(true)
                .verifiedAt(LocalDateTime.now())
                .build();
        return corpRepository.save(corp);
    }

    @Override
    public Corp bisCheck(CorpDto dto) {
        return corpRepository.findByBisNo(dto.getBisNo().replaceAll("\\D", ""))
                .orElseThrow(() -> new IllegalArgumentException("등록된 기업을 찾을 수 없습니다."));
    }

    @Override
    public boolean isVerified(String bisNo) {
        return corpRepository.findByBisNo(bisNo.replaceAll("\\D", ""))
                .map(Corp::isVerified).orElse(false);
    }
}
