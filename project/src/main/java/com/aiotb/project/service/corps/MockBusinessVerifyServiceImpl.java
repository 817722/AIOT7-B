package com.aiotb.project.service.corps;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 발표/시연 전용 사업자 인증 Mock.
 *
 * nts.api.mock-enabled=true 프로필에서만 등록된다.
 * 설정된 샘플 사업자번호가 입력되면 실제 외부 API를 호출하지 않고
 * 국세청 API와 비슷한 응답을 반환한다. 다른 번호는 불일치로 반환한다.
 *
 */
@Service
@Profile("demo")
public class MockBusinessVerifyServiceImpl implements BusinessVerifyService {

    @Override
    public Map<?, ?> verify(String businessNo, String openDate, String representativeName) {
        String normalizedNo = businessNo == null ? "" : businessNo.replaceAll("\\D", "");
        boolean matched = "1234567890".equals(normalizedNo);
        Map<String, String> item = Map.of(
                "b_no", normalizedNo,
                "valid", matched ? "01" : "02",
                "valid_msg", matched ? "확인 완료 (일치)" : "확인 불가 (불일치)");
        return Map.of("status_code", "OK", "data", List.of(item));
    }
}
