package com.aiotb.project.service.corps;

import java.util.Map;

/**
 * 사업자등록정보 진위확인 서비스의 공통 인터페이스.
 * 실제 국세청 API와 발표용 Mock 구현체가 같은 방식으로 호출되도록 한다.
 */
public interface BusinessVerifyService {

    /**
     * 사업자등록번호, 개업일자, 대표자명을 확인한다.
     *
     * @param businessNo         사업자등록번호(숫자 10자리)
     * @param openDate           개업일자(yyyy-MM-dd 또는 yyyyMMdd)
     * @param representativeName 대표자명
     * @return 국세청 API와 호환되는 응답 구조
     */
    Map<?, ?> verify(String businessNo, String openDate, String representativeName);
}
