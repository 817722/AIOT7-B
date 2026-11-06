package com.aiotb.project.service.corps;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Mock, 발표 전용
 * 설정된 샘플 사업자번호가 입력되면 실제 [사업자등록정보 진위확인 및 상태조회 API] 호출을 하지 않는다.
 * Mock 을 이용하려면 반드시 application-demo.properties 가 실행되어야 하므로 docs/README.md 에 사용 방법 참고.
 */
@Service
@Profile("demo")
public class MockBusinessVerifyServiceImpl implements BusinessVerifyService {

    @Override
    public Map<?, ?> verify(String businessNo, String openDate, String representativeName) {
        String normalizedNo = businessNo == null ? "" : businessNo.replaceAll("\\D", "");
        // Mock용 사업자 번호를 추가하고 싶다면 다음과 같이 작성한다. 
        // boolean matched = "1234567890".equals(normalizedNo) || "원하는사업자번호".equals(normalizedNo);
        boolean matched = "1234567890".equals(normalizedNo) || "1234123411".equals(normalizedNo) || "1234123422".equals(normalizedNo) ;
        Map<String, String> item = Map.of(
                "b_no", normalizedNo,
                "valid", matched ? "01" : "02",
                "valid_msg", matched ? "확인 완료 (일치)" : "확인 불가 (불일치)");
        return Map.of("status_code", "OK", "data", List.of(item));
    }
}
