package com.aiotb.project.controller.users;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 기존 "관리자 검수 상세"(/users/{no}) 경로.
 * 하드코딩 시연 데이터(SAMPLE_BRIDGES 등)를 제거하고 권한별 실제 사용자 상세 화면으로 연결한다.
 * - SUPER_ADMIN → /admin/users/{no} (전체 사용자 조회)
 * - ADMIN       → /corps/members/{no} (소속 기업 사용자만, 다른 기업은 404)
 */
@Controller
public class UserDetailPageController {

    @GetMapping("/users/{no}")
    public String detail(@PathVariable("no") Long no, Authentication authentication) {
        String home = UserPageController.adminHome(authentication);
        return switch (home) {
            case "/admin" -> "redirect:/admin/users/" + no;
            case "/corps" -> "redirect:/corps/members/" + no;
            default -> "redirect:/bridges";
        };
    }
}
