package com.aiotb.project.service.corps;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.domain.corps.Corp;
import com.aiotb.project.dto.corps.CorpDto;
import com.aiotb.project.repository.corps.CorpRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorpServiceImpl implements CorpService {
    private final CorpRepository corpRepository;
    private final BusinessVerifyService verificationService;

    @Override
    @Transactional
    public Corp verifyAndRegister(CorpDto dto) {
        String bisNo = dto.getBisNo() == null ? "" : dto.getBisNo().replaceAll("\\D", "");
        if (!bisNo.matches("\\d{10}") || dto.getOpenDate() == null ||
                dto.getCeoName() == null || dto.getCeoName().isBlank() ||
                dto.getCorpName() == null || dto.getCorpName().isBlank()) {
            throw new IllegalArgumentException("기업 정보를 올바르게 입력해 주세요.");
        }

        String ceoName = dto.getCeoName().trim();
        String corpName = dto.getCorpName().trim();

        // 같은 사업자번호의 기업은 한 번만 생성한다. 기존에 인증된 기업이면 새 사용자가 해당 기업에 소속될 수 있도록 재사용한다.
        Corp existing = corpRepository.findByBisNo(bisNo).orElse(null);
        if (existing != null) {
            // 사업자번호만 같고 나머지 기업 정보가 다른 경우에는 기존 기업에 임의로 연결하지 않는다.
            boolean sameDetails = existing.isVerified()
                    && existing.getOpenDate().equals(dto.getOpenDate())
                    && existing.getCeoName().equalsIgnoreCase(ceoName)
                    && existing.getCorpName().equalsIgnoreCase(corpName);
            if (!sameDetails) {
                throw new IllegalArgumentException("등록된 기업 정보와 일치하지 않습니다. 기업 관리자에게 계정 등록을 요청해 주세요.");
            }
            return existing;
        }

        // 최초 기업 등록 시에만 외부 사업자 진위확인을 수행한다.
        Map<?, ?> response = verificationService.verify(bisNo, dto.getOpenDate().toString(), ceoName);
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
                .ceoName(ceoName)
                .corpName(corpName)
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
