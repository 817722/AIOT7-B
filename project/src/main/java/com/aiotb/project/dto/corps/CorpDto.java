package com.aiotb.project.dto.corps;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// 기업 정보 DTO
@Data
public class CorpDto {

    @NotBlank(message = "사업자등록번호를 입력해 주세요.")
    private String bisNo;

    @NotBlank(message = "대표자명을 입력해 주세요.")
    private String ceoName;

    @NotBlank(message = "기업명을 입력해 주세요.")
    private String corpName;

    @NotNull(message = "개업일자를 입력해 주세요.")
    private LocalDate openDate;

    private String phone;

    private String email;

}