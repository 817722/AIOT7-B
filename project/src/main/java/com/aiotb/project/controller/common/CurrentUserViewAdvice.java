package com.aiotb.project.controller.common;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * 공통 Thymeleaf 헤더에 현재 로그인한 사용자 담당자명을 전달한다.
 * 기존 CustomUserDetailsService는 principal.name에 로그인 ID를 넣으므로
 * users.manager_name을 조회하여 표시한다.
 *
 * 인증 없는 로그인/회원가입 화면에서는 currentWorkerName을 만들지 않는다.
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class CurrentUserViewAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("currentWorkerName")
    public String currentWorkerName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(User::getManagerName)
                .filter(name -> !name.isBlank())
                .orElse(username);
    }

    /** Spring Security 권한에 따라 공통 헤더에 표시할 직책을 결정한다. */
    @ModelAttribute("currentWorkerRoleLabel")
    public String currentWorkerRoleLabel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        boolean superAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
        if (superAdmin) return "최고관리자";

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return admin ? "관리자" : "작업자";
    }

    /**
     * 사이드바 메뉴 표시용 대표 권한: SUPER_ADMIN / ADMIN / INSPECTOR (비로그인 시 null).
     * 메뉴 표시 여부일 뿐이며, 실제 접근 제한은 SecurityConfig와 Controller/Service에서 검증한다.
     */
    @ModelAttribute("currentUserRole")
    public String currentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        java.util.Set<String> roles = new java.util.HashSet<>();
        authentication.getAuthorities().forEach(a -> roles.add(a.getAuthority()));
        if (roles.contains("ROLE_SUPER_ADMIN")) return "SUPER_ADMIN";
        if (roles.contains("ROLE_ADMIN")) return "ADMIN";
        if (roles.contains("ROLE_INSPECTOR")) return "INSPECTOR";
        return null;
    }
}
