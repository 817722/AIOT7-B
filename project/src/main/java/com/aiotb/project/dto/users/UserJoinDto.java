package com.aiotb.project.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 회원가입 폼 DTO
@Data
public class UserJoinDto {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(min = 4, max = 15, message = "아이디는 4~15자 이내로 입력해야 합니다.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 6, message = "비밀번호는 6자 이상 입력해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호를 확인해 주세요.")
    private String passwordConfirm;

    @NotBlank(message = "이름을 입력해 주세요.")
    private String name;

    @NotBlank (message = "전화번호를 입력해 주세요.")
    private String phone;

}
