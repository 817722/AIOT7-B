package com.aiotb.project.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 회원가입 폼 DTO
@Data
public class UserJoinDto {

    @NotBlank(message = "아이디를 입력하세요.")
    @Size(min = 4, max = 15, message = "아이디는 4~15자로 입력하세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 6, message = "비밀번호는 6자 이상 입력하세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordConfirm;

    @NotBlank(message = "이름을 입력하세요.")
    private String name;

    private String phone;

}
