package com.aiotb.project.service.corps;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 실제 국세청 사업자등록정보 진위확인 API 구현체.
 *
 * nts.api.mock-enabled=true인 경우에는 이 빈을 등록하지 않고
 * MockBusinessVerifyServiceImpl이 대신 등록된다.
 */
@Service
@ConditionalOnProperty(name = "nts.api.mock-enabled", havingValue = "false", matchIfMissing = true)
public class BusinessVerificationService implements BusinessVerifyService {

        private final RestClient restClient;
        private final String serviceKey;

        public BusinessVerificationService(
                        RestClient.Builder builder,
                        @Value("${nts.api.base-url}") String baseUrl,
                        @Value("${nts.api.service-key:}") String serviceKey) {
                this.restClient = builder.baseUrl(baseUrl).build();
                this.serviceKey = serviceKey;
        }

        @Override
        public Map<?, ?> verify(String businessNo, String openDate, String representativeName) {
                // 국세청 API는 사업자번호와 개업일자를 숫자만 있는 형식으로 받는다.
                Map<String, Object> business = Map.of(
                                "b_no", businessNo.replaceAll("\\D", ""),
                                "start_dt", openDate.replaceAll("\\D", ""),
                                "p_nm", representativeName);

                Map<String, Object> request = Map.of("businesses", List.of(business));

                return restClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/validate")
                                                .queryParam("serviceKey", serviceKey)
                                                .build())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(request)
                                .retrieve()
                                .body(Map.class);
        }
}
