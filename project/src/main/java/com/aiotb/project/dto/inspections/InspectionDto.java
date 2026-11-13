package com.aiotb.project.dto.inspections;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.domain.inspections.Inspection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class InspectionDto {

    /** 📝 점검 등록/수정 통합 요청 DTO (파일 업로드 및 세부 항목 포함) */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        @NotNull(message = "교량 번호는 필수입니다.")
        private Long bridgeNo;

        // 기존 클라이언트 호환을 위해 필드는 유지하지만 서버에서는 로그인 사용자 기업 사용
        private Long corpNo;

        @NotBlank(message = "점검 기록명은 필수입니다.")
        private String title;

        @NotBlank(message = "점검 유형은 필수입니다.")
        private String inspectionType;

        private String status;
        private String inspector; // 담당자 이름 또는 정보 (문자열 호환용)

        @NotNull(message = "점검 날짜는 필수입니다.")
        private LocalDate inspectionDate;

        @NotNull(message = "점검 담당자 번호는 필수입니다.")
        private Long inspectorNo;

        // 기존 메모 및 상세 평가 필드 통합
        private String memo;
        private String safetyMemo;   // 안전 점검 메모
        private String opinion;      // 검토 의견
        private String risk;         // 위험도
        private String summary;      // 결과 요약
        private String overallResult;
        private String reviewOpinion;

        // 🎥 드론 영상 업로드 필드 통합 (단일 & 다중)
        private MultipartFile videoInputSingle;
        private List<MultipartFile> videoInputMultiple;

        // 서버에서 로그인 사용자 번호로 대체 (클라이언트 값 무시)
        private Long createdBy;

        // 롬복이 혹시 인식하지 못할 경우를 대비한 안전망 겟터
        public MultipartFile getVideoInputSingle() {
            return this.videoInputSingle;
        }

        public List<MultipartFile> getVideoInputMultiple() {
            return this.videoInputMultiple;
        }
    }

    /** 📊 점검 조회 응답 DTO */
    @Getter
    public static class Response {
        private Long no;
        private String id;
        private Long bridgeNo;
        private String bridgeName;
        private String title;
        private String inspectionType;
        private String status;
        private LocalDate inspectionDate;
        private Long inspectorNo;
        private String memo;
        private String overallResult;

        public Response(Inspection inspection) {
            this.no = inspection.getNo();
            this.id = inspection.getId();
            this.bridgeNo = inspection.getBridge().getNo();
            this.bridgeName = inspection.getBridge().getName();
            this.title = inspection.getTitle();
            this.inspectionType = inspection.getInspectionType();
            this.status = inspection.getStatus();
            this.inspectionDate = inspection.getInspectionDate();
            this.inspectorNo = inspection.getInspectorNo();
            this.memo = inspection.getInspectionMemo();
            this.overallResult = inspection.getOverallResult();
        }
    }
}