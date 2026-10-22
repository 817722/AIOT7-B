package com.aiotb.project.dto.defects;

import com.aiotb.project.domain.defects.Defect;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class DefectDto {

    // 결함 등록/수정 요청 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        @NotNull(message = "점검 번호는 필수입니다.")
        private Long inspectionNo;

        @NotBlank(message = "결함 유형은 필수입니다.")
        private String defectType;

        @NotBlank(message = "결함 위치는 필수입니다.")
        private String location;

        private String severity; // 낮음, 보통, 높음, 긴급
        private String description;

        @NotNull(message = "등록자 번호는 필수입니다.")
        private Long createdBy;
    }

    // 결함 조회 응답 DTO
    @Getter
    public static class Response {
        private Long no;
        private String id;
        private Long inspectionNo;
        private String defectType;
        private String location;
        private String severity;
        private String description;

        public Response(Defect defect) {
            this.no = defect.getNo();
            this.id = defect.getId();
            this.inspectionNo = defect.getInspection().getNo();
            this.defectType = defect.getDefectType();
            this.location = defect.getLocation();
            this.severity = defect.getSeverity();
            this.description = defect.getDescription();
        }
    }
}