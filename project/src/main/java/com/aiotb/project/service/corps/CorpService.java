package com.aiotb.project.service.corps;

import com.aiotb.project.domain.corps.Corp;
import com.aiotb.project.dto.corps.CorpDto;

public interface CorpService {

    // 기업 인증 및 등록
    Corp verifyAndRegister(CorpDto dto);

    // 사업자등록번호로 기업 조회
    Corp bisCheck(CorpDto dto);

    // 기업 인증 여부 확인
    boolean isVerified(String bisNo);

}
