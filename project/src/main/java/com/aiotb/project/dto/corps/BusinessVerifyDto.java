package com.aiotb.project.dto.corps;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class BusinessVerifyDto {

    @Getter @Setter
    public static class Request {
        private List<BusinessData> data;
    }

    @Getter @Setter
    public static class BusinessData {
        private String b_no;        // 사업자등록번호 (10자리)
        private String start_dt;    // 개업일자 (YYYYMMDD)
        private String p_nm;        // 대표자명
        private String b_nm;        // 상호명 (선택)
    }

    @Getter @Setter
    public static class Response {
        private Integer request_cnt;
        private Integer valid_cnt;
        private String status_code;
        private List<ResponseData> data;
    }

    @Getter @Setter
    public static class ResponseData {
        private String b_no;
        private String valid;       // "01" 이면 일치
        private String valid_msg;   // "확인 완료 (일치)"
        private Status status;
    }

    @Getter @Setter
    public static class Status {
        private String b_no;
        private String b_stt;       // "계속사업자"
        private String b_stt_cd;    // "01"
        private String tax_type;
        private String tax_type_cd;
    }
}