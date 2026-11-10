package com.aiotb.project.dto.tests;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

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
