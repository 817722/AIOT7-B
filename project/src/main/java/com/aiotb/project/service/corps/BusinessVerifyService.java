package com.aiotb.project.service.corps;

import java.util.Map;

/** 사업자 진위확인 공통 인터페이스. 
 * 기본 실행은 실제 API, demo 프로필은 Mock을 사용한다. 
 */
public interface BusinessVerifyService {
    Map<?, ?> verify(String businessNo, String openDate, String representativeName);
}

