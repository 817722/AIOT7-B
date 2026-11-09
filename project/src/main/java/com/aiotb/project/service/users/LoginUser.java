package com.aiotb.project.service.users;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.aiotb.project.domain.corps.Corp;
import com.aiotb.project.domain.users.User.Status;

/**
 * Spring Security 로그인 사용자(principal).
 * 기존과 동일하게 getUsername()은 로그인 ID를 반환하며,
 * 승인 상태 검사를 위해 사용자/기업 상태와 번호를 함께 보관한다.
 */
public class LoginUser extends User {
    private final long userNo;
    private final long corpNo;
    private final Status userStatus;
    private final Corp.Status corpStatus;

    public LoginUser(String username, String password, Collection<? extends GrantedAuthority> authorities,
                     long userNo, long corpNo, Status userStatus, Corp.Status corpStatus) {
        super(username, password, authorities);
        this.userNo = userNo;
        this.corpNo = corpNo;
        this.userStatus = userStatus;
        this.corpStatus = corpStatus;
    }

    public long getUserNo() { return userNo; }
    public long getCorpNo() { return corpNo; }
    public Status getUserStatus() { return userStatus; }
    public Corp.Status getCorpStatus() { return corpStatus; }
}
