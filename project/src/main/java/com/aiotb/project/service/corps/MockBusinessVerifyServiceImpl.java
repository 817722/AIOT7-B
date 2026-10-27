package com.aiotb.project.service.corps;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.aiotb.project.dto.corps.BusinessVerifyDto;

@Service
public class MockBusinessVerifyServiceImpl implements BusinessVerifyService {

    @Override
    public BusinessVerifyDto.Response verifyBusiness(BusinessVerifyDto.Request request) {
        BusinessVerifyDto.Response response = new BusinessVerifyDto.Response();
        List<BusinessVerifyDto.ResponseData> responseDataList = new ArrayList<>();

        for (BusinessVerifyDto.BusinessData reqData : request.getData()) {
            BusinessVerifyDto.ResponseData resData = new BusinessVerifyDto.ResponseData();
            resData.setB_no(reqData.getB_no());

            // 테스트용 번호 입력 시 무조건 "일치" 데이터 생성 : 1234567890
            if ("1234567890".equals(reqData.getB_no())) {
                resData.setValid("01");
                resData.setValid_msg("확인 완료 (일치)");

                BusinessVerifyDto.Status status = new BusinessVerifyDto.Status();
                status.setB_no(reqData.getB_no());
                status.setB_stt("계속사업자");
                status.setB_stt_cd("01");
                status.setTax_type("부가가치세 일반과세자");
                status.setTax_type_cd("01");
                resData.setStatus(status);
            } else {
                // 그 외 번호는 불일치 처리
                resData.setValid("02");
                resData.setValid_msg("확인 불가 (불일치)");
            }
            responseDataList.add(resData);
        }

        response.setRequest_cnt(request.getData().size());
        response.setValid_cnt("0000000000".equals(request.getData().get(0).getB_no()) ? 1 : 0);
        response.setStatus_code("OK");
        response.setData(responseDataList);

        return response;
    }
}
