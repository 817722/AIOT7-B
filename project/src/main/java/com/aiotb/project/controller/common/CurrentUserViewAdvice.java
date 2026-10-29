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
}
