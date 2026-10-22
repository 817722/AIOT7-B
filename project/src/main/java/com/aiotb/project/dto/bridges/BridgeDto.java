package com.aiotb.project.dto.bridges;

import com.aiotb.project.domain.bridges.Bridge;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

public class BridgeDto {

    // 등록 및 수정 요청 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        @NotBlank(message = "교량명은 필수 항목입니다.")
        private String name;

        private String facilityType; // 제1종, 제2종, 제3종
        private String province; // 시도명
        private String city; // 시군구명
        private String address; // 주소
        private BigDecimal bridgeLength; // 연장
        private BigDecimal bridgeWidth; // 폭
        private String managementAgency; // 관리기관명
        private Long createdBy; // 등록자 PK
    }

    // 목록/상세 조회 응답 DTO
    @Getter
    public static class Response {
        private Long no;
        private String id;
        private String name;
        private String facilityType;
        private String province;
        private String city;
        private String address;
        private BigDecimal bridgeLength;
        private BigDecimal bridgeWidth;
        private String managementAgency;

        public Response(Bridge bridge) {
            this.no = bridge.getNo();
            this.id = bridge.getId();
            this.name = bridge.getName();
            this.facilityType = bridge.getFacilityType();
            this.province = bridge.getProvince();
            this.city = bridge.getCity();
            this.address = bridge.getAddress();
            this.bridgeLength = bridge.getBridgeLength();
            this.bridgeWidth = bridge.getBridgeWidth();
            this.managementAgency = bridge.getManagementAgency();
        }
    }
}