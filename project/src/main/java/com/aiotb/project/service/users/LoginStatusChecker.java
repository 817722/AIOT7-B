package com.aiotb.project.service.users;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

import com.aiotb.project.domain.corps.Corp;
import com.aiotb.project.domain.users.User;

/**
 * 비밀번호 검증이 끝난 뒤(postAuthenticationChecks) 승인 상태를 검사한다.
 * 비밀번호를 모르는 사람에게는 계정의 승인 상태가 노출되지 않는다.
 *
 * 예외 메시지는 로그인 화면에서 안내 문구를 고르기 위한 코드로 사용한다.
 * - DisabledException: 사용자 상태(users.status) 문제 → 기업 ADMIN 승인 필요
 * - LockedException  : 기업 상태(corps.status) 문제 → SUPER_ADMIN 승인/정지 해제 필요
 */
public class LoginStatusChecker implements UserDetailsChecker {

    @Override
    public void check(UserDetails details) {
        if (!(details instanceof LoginUser user)) {
            throw new DisabledException("USER_DISABLED");
        }
        if (user.getCorpStatus() != Corp.Status.ACTIVE) {
            throw new LockedException("CORP_" + user.getCorpStatus().name());
        }
        if (user.getUserStatus() != User.Status.ACTIVE) {
            throw new DisabledException("USER_" + user.getUserStatus().name());
        }
    }
}
