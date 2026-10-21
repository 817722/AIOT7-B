package com.aiotb.project.service.users;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.dto.users.UserJoinDto;

public interface UserService {

    // 아이디 사용 가능 여부 (아이디 중복 검사)
    boolean isUsernameAvailable(String username);

    // 회원 가입
    User join(UserJoinDto dto);

    // 회원 조회
    User select(String username);
}
