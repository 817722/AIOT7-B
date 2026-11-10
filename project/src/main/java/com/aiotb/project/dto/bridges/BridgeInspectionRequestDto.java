package com.aiotb.project.dto.bridges;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter 
@Setter 
public class BridgeInspectionRequestDto {
    private String keyword;         // 1. 교량명
    private String inspectionType;  // 2. 점검분류
    private String inspector;       // 3. 담당자
    private String status;          // 4. 점검 진행 상태
    
    // 5. 검측 체크리스트 (필요에 따라 DTO 리스트로 확장 가능)
    private List<ChecklistItemDto> checklist; 
    // 6. 안전 점검표 체크박스 값들
    private List<String> safetyChecklist;

    private String safetyMemo;      // 7. 안전 점검 메모
    private String opinion;         // 8-1. 검토 의견
    private String risk;            // 8-2. 위험도
    private String summary;         // 8-3. 결과 요약

    // 9. 드론 영상 업로드 (단일)
    private MultipartFile videoInputSingle;      
    // 10. 파일 추가 (다중)
    private List<MultipartFile> videoInputMultiple;

    // 👇 롬복이 안 먹힐 때를 대비해 우리가 직접 만든 겟터 메서드입니다! (이게 있으면 빨간 줄이 절대 안 뜹니다)
    public List<MultipartFile> getVideoInputMultiple() {
        return this.videoInputMultiple;
    }

    public MultipartFile getVideoInputSingle() {
        return this.videoInputSingle;
    }
}