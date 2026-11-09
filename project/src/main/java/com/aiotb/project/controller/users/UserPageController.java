package com.aiotb.project.controller.users;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 기존 "관리자 검수"(/users) 진입 경로.
 * 이전에는 교량 목록을 복사한 하드코딩 예시 데이터(SAMPLE_BRIDGES)를 표시했으나,
 * 실제 관리자 기능이 /admin(웹 관리자), /corps(기업 관리자)로 구현되어 권한별 화면으로 연결한다.
 * 이동한 화면의 접근 권한은 SecurityConfig와 각 Controller에서 다시 검증한다.
 */
@Controller
public class UserPageController {

    @GetMapping("/users")
    public String list(Authentication authentication) {
        return "redirect:" + adminHome(authentication);
    }

    static String adminHome(Authentication authentication) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) return "/admin";
        if (hasRole(authentication, "ROLE_ADMIN")) return "/corps";
        return "/bridges"; // INSPECTOR: 관리자 화면 접근 권한 없음
    }

    private static boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }
}
