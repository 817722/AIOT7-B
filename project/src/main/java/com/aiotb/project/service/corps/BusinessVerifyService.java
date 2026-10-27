package com.aiotb.project.service.corps;

import com.aiotb.project.dto.corps.BusinessVerifyDto;

public interface BusinessVerifyService {
    BusinessVerifyDto.Response verifyBusiness(BusinessVerifyDto.Request request);
}
