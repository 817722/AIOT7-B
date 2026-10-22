package com.aiotb.project.dto.inspections;

import com.aiotb.project.domain.inspections.Inspection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

public class InspectionDto {

    // 점검 등록/수정 요청 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        @NotNull(message = "교량 번호는 필수입니다.")
        private Long bridgeNo;

        @NotNull(message = "기업 번호는 필수입니다.")
        private Long corpNo;

        @NotBlank(message = "점검 유형은 필수입니다.")
        private String inspectionType;

        private String status;

        @NotNull(message = "점검 날짜는 필수입니다.")
        private LocalDate inspectionDate;

        @NotNull(message = "점검 담당자 번호는 필수입니다.")
        private Long inspectorNo;

        private String overallResult;
        private String reviewOpinion;

        @NotNull(message = "등록자 번호는 필수입니다.")
        private Long createdBy;
    }

    // 점검 조회 응답 DTO
    @Getter
    public static class Response {
        private Long no;
        private String id;
        private Long bridgeNo;
        private String bridgeName;
        private String inspectionType;
        private String status;
        private LocalDate inspectionDate;
        private Long inspectorNo;
        private String overallResult;

        public Response(Inspection inspection) {
            this.no = inspection.getNo();
            this.id = inspection.getId();
            this.bridgeNo = inspection.getBridge().getNo();
            this.bridgeName = inspection.getBridge().getName();
            this.inspectionType = inspection.getInspectionType();
            this.status = inspection.getStatus();
            this.inspectionDate = inspection.getInspectionDate();
            this.inspectorNo = inspection.getInspectorNo();
            this.overallResult = inspection.getOverallResult();
        }
    }
}