package com.aiotb.project.service.corps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "nts.api.mock-enabled", havingValue = "true")
public class MockBusinessVerifyServiceImpl implements BusinessVerifyService {

    private final String sampleBusinessNo;

    public MockBusinessVerifyServiceImpl(
            @Value("${nts.api.mock-business-no:1234567890}") String sampleBusinessNo) {
        this.sampleBusinessNo = sampleBusinessNo.replaceAll("\\D", "");
    }

    @Override
    public Map<?, ?> verify(String businessNo, String openDate, String representativeName) {
        String normalizedBusinessNo = businessNo == null ? "" : businessNo.replaceAll("\\D", "");
        boolean matched = sampleBusinessNo.equals(normalizedBusinessNo);

        Map<String, Object> item = new HashMap<>();
        item.put("b_no", normalizedBusinessNo);
        item.put("valid", matched ? "01" : "02");
        item.put("valid_msg", matched ? "확인 완료 (발표용 샘플 데이터)" : "확인 불가 (샘플 번호 불일치)");

        if (matched) {
            Map<String, Object> status = new HashMap<>();
            status.put("b_no", normalizedBusinessNo);
            status.put("b_stt", "계속사업자");
            status.put("b_stt_cd", "01");
            status.put("tax_type", "부가가치세 일반과세자");
            status.put("tax_type_cd", "01");
            item.put("status", status);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("request_cnt", 1);
        response.put("valid_cnt", matched ? 1 : 0);
        response.put("status_code", "OK");
        response.put("data", List.of(item));
        return response;
    }
}
