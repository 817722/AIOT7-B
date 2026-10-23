
package com.aiotb.project.service.corps;

import java.util.Map;
import java.util.List;

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
    private final BusinessVerificationService businessVerificationService;

    // 기업 인증 및 등록
    @Override
    @Transactional
    public Corp verifyAndRegister(CorpDto dto) {

        String bisNo = dto.getBisNo().replace("-", "");

        // 이미 등록된 기업인지 확인
        if (corpRepository.findByBisNo(bisNo).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        // 국세청 API 검증
        Map<?, ?> response = businessVerificationService.verify(
                bisNo,
                dto.getOpenDate().toString(),
                dto.getCeoName());

        // 검증 결과 확인
        Object dataObject = response.get("data");

        if (!(dataObject instanceof List<?> dataList) || dataList.isEmpty()) {
            throw new IllegalArgumentException("기업 인증 결과를 확인할 수 없습니다.");
        }

        Object firstObject = dataList.get(0);

        if (!(firstObject instanceof Map<?, ?> result)) {
            throw new IllegalArgumentException("기업 인증 응답 형식이 올바르지 않습니다.");
        }

        if (!"02".equals(String.valueOf(result.get("valid")))) {
            throw new IllegalArgumentException("기업 인증에 실패했습니다.");
        }

        // 기업 정보 저장
        Corp corp = Corp.builder()
                .bisNo(bisNo)
                .ceoName(dto.getCeoName())
                .corpName(dto.getCorpName())
                .openDate(dto.getOpenDate())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .verified(true)
                .build();

        return corpRepository.save(corp);
    }

    // 사업자등록번호로 기업 조회
    @Override
    public Corp bisCheck(CorpDto dto) {
        String bisNo = dto.getBisNo().replace("-", "");

        return corpRepository.findByBisNo(bisNo)
                .orElseThrow(() -> new IllegalArgumentException("등록된 기업을 찾을 수 없습니다."));
    }

    // 기업 인증 여부 확인
    @Override
    public boolean isVerified(String bisNo) {
        String normalizedBisNo = bisNo.replace("-", "");

        return corpRepository.findByBisNo(normalizedBisNo)
                .map(Corp::isVerified)
                .orElse(false);
    }
}
