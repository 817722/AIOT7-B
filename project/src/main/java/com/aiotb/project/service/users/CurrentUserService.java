package com.aiotb.project.service.users;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.domain.users.UserAuth;
import com.aiotb.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * REST API에서 "누가 요청했는가"를 서버 기준으로 확인한다.
 * 클라이언트가 보낸 corpNo / createdBy / uploadedBy 값은 신뢰하지 않고 이 결과만 사용한다.
 * 로그인 이후 기업이 정지되거나 계정이 비활성화된 경우도 매 요청 DB 상태로 다시 확인한다.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    /** 요청 사용자 정보: 사용자 번호, 소속 기업 번호, 권한 */
    public record Actor(long userNo, long corpNo, boolean admin, boolean superAdmin) { }

    @Transactional(readOnly = true)
    public Actor current() {
        return current(SecurityContextHolder.getContext().getAuthentication());
    }

    @Transactional(readOnly = true)
    public Actor current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("사용자 정보를 찾을 수 없습니다."));
        if (!user.canUseService()) {
            throw new AccessDeniedException("승인되지 않았거나 이용이 정지된 계정입니다.");
        }
        return new Actor(user.getNo(), user.getCorp().getNo(),
                user.hasRole(UserAuth.Role.ADMIN), user.hasRole(UserAuth.Role.SUPER_ADMIN));
    }
}
