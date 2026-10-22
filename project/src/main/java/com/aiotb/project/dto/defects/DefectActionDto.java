package com.aiotb.project.dto.defects;

import com.aiotb.project.domain.defects.DefectAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DefectActionDto {

    // 조치 이력 등록/수정 요청 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        @NotNull(message = "결함 번호는 필수입니다.")
        private Long defectNo;

        private Long maintenanceNo;

        @NotBlank(message = "처리 상태는 필수입니다.")
        private String status; // 발견, 조치 필요, 조치 중, 조치 완료

        private LocalDate actionDate;

        @NotNull(message = "담당자 번호는 필수입니다.")
        private Long managerNo;

        private String actionDescription;
        private String result;
        private LocalDateTime completedAt;

        @NotNull(message = "등록자 번호는 필수입니다.")
        private Long createdBy;
    }

    // 조치 이력 조회 응답 DTO
    @Getter
    public static class Response {
        private Long no;
        private String id;
        private Long defectNo;
        private Long maintenanceNo;
        private String status;
        private LocalDate actionDate;
        private Long managerNo;
        private String actionDescription;
        private String result;
        private LocalDateTime completedAt;

        public Response(DefectAction action) {
            this.no = action.getNo();
            this.id = action.getId();
            this.defectNo = action.getDefectNo();
            this.maintenanceNo = action.getMaintenanceNo();
            this.status = action.getStatus();
            this.actionDate = action.getActionDate();
            this.managerNo = action.getManagerNo();
            this.actionDescription = action.getActionDescription();
            this.result = action.getResult();
            this.completedAt = action.getCompletedAt();
        }
    }
}