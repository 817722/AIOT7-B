package com.aiotb.project.service.corps;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BusinessVerificationService {

        private final RestClient restClient;
        private final String serviceKey;

        public BusinessVerificationService(
                        RestClient.Builder builder,
                        @Value("${nts.api.base-url}") String baseUrl,
                        @Value("${nts.api.service-key}") String serviceKey) {

                this.restClient = builder.baseUrl(baseUrl).build();
                this.serviceKey = serviceKey;
        }

        public Map<?, ?> verify(
                        String businessNo,
                        String openDate,
                        String representativeName) {

                Map<String, Object> business = Map.of(
                                "b_no", businessNo.replace("-", ""),
                                "start_dt", openDate.replace("-", ""),
                                "p_nm", representativeName);

                Map<String, Object> request = Map.of(
                                "businesses", List.of(business));

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