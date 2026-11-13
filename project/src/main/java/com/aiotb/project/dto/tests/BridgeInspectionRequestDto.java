package com.aiotb.project.dto.tests;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

// 이건 Postman으로 테스트 용 코드라 있어도 무방하고 없어도 상관없는 코드입니다.
// 신경 안 써도 될 듯.

@Getter 
@Setter 
public class BridgeInspectionRequestDto {
    private String keyword;
    private String inspectionType;
    private String inspector;
    private String status;
    private List<String> safetyChecklist ;
    private String safetyMemo;
    private String opinion;
    private String risk;
    private String summary;
    private MultipartFile videoInputSingle;
    private List<MultipartFile> videoInputMultiple;
}
