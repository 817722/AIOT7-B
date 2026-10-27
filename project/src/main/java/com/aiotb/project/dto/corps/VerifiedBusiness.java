package com.aiotb.project.dto.corps;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** 국세청 검증에 성공한 신규 기업 정보(서버 HttpSession에만 저장). */
public record VerifiedBusiness(
        String businessNo,
        String representativeName,
        String corpName,
        LocalDate openDate,
        Instant verifiedAt
) implements Serializable {
    public static final String SESSION_KEY = "VERIFIED_BUSINESS_FOR_SIGNUP";
    public boolean expired() {
        return verifiedAt == null || verifiedAt.isBefore(Instant.now().minus(10, ChronoUnit.MINUTES));
    }
}
