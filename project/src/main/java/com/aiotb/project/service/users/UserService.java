package com.aiotb.project.service.users;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.dto.corps.VerifiedBusiness;
import com.aiotb.project.dto.users.UserJoinDto;

public interface UserService {
    boolean isUsernameAvailable(String username);
    User join(UserJoinDto dto, VerifiedBusiness verifiedBusiness);
    User select(String username);
}
