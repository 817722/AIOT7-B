package com.aiotb.project.controller.admin;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aiotb.project.service.admin.AdminService;
import com.aiotb.project.service.admin.AdminService.CorpAction;
import com.aiotb.project.service.users.CurrentUserService;

import lombok.RequiredArgsConstructor;

/**
 * 웹 관리자(SUPER_ADMIN) 페이지.
 * 접근 제한: SecurityConfig(/admin/** → SUPER_ADMIN) + @PreAuthorize + AdminService 내부 검사
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminPageController {
    private final AdminService adminService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAllAttributes(adminService.dashboard(currentUserService.current()));
        menu(model);
        return "page/admin/dashboard";
    }

    @GetMapping("/corps")
    public String corps(@RequestParam(name = "status", defaultValue = "") String status,
                        @RequestParam(name = "keyword", defaultValue = "") String keyword,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        Model model) {
        AdminService.Page result = adminService.corps(currentUserService.current(), status, keyword, page);
        model.addAttribute("result", result);
        model.addAttribute("counts", adminService.corpCounts(keyword));
        model.addAttribute("status", normalize(status, "PENDING", "ACTIVE", "REJECTED", "SUSPENDED"));
        model.addAttribute("keyword", keyword.strip());
        menu(model);
        return "page/admin/corps";
    }

    @GetMapping("/corps/{no}")
    public String corp(@PathVariable("no") long no, Model model) {
        model.addAttribute("corp", adminService.corp(currentUserService.current(), no));
        menu(model);
        return "page/admin/corp-detail";
    }

    /** 기업 가입 승인·반려, 이용 정지·해제 (POST + CSRF) */
    @PostMapping("/corps/{no}/status")
    public String changeCorpStatus(@PathVariable("no") long no,
                                   @RequestParam("action") CorpAction action,
                                   @RequestParam(name = "reason", required = false) String reason,
                                   RedirectAttributes redirect) {
        try {
            adminService.changeCorpStatus(currentUserService.current(), no, action, reason);
            redirect.addFlashAttribute("adminMessage", switch (action) {
                case APPROVE -> "기업 가입을 승인했습니다. 기업 관리자가 로그인할 수 있습니다.";
                case REJECT -> "기업 가입을 반려했습니다.";
                case SUSPEND -> "기업의 서비스 이용을 정지했습니다. 소속 사용자는 로그인할 수 없으며 데이터는 유지됩니다.";
                case RESTORE -> "기업의 이용 정지를 해제했습니다.";
            });
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("adminError", e.getMessage());
        }
        return "redirect:/admin/corps/" + no;
    }

    @GetMapping("/users")
    public String users(@RequestParam(name = "corpNo", required = false) Long corpNo,
                        @RequestParam(name = "status", defaultValue = "") String status,
                        @RequestParam(name = "role", defaultValue = "") String role,
                        @RequestParam(name = "keyword", defaultValue = "") String keyword,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        Model model) {
        CurrentUserService.Actor actor = currentUserService.current();
        model.addAttribute("result", adminService.users(actor, corpNo, status, role, keyword, page));
        model.addAttribute("corpOptions", adminService.corpOptions(actor));
        model.addAttribute("corpNo", corpNo);
        model.addAttribute("status", normalize(status, "PENDING", "ACTIVE", "REJECTED", "DISABLED"));
        model.addAttribute("role", normalize(role, "SUPER_ADMIN", "ADMIN", "INSPECTOR"));
        model.addAttribute("keyword", keyword.strip());
        menu(model);
        return "page/admin/users";
    }

    @GetMapping("/users/{no}")
    public String user(@PathVariable("no") long no, Model model) {
        Map<String, Object> user = adminService.user(currentUserService.current(), no);
        model.addAttribute("user", user);
        menu(model);
        return "page/admin/user-detail";
    }

    private static String normalize(String value, String... allowed) {
        for (String a : allowed) if (a.equals(value)) return a;
        return "";
    }

    private static void menu(Model model) {
        model.addAttribute("activeMenu", "admin");
        model.addAttribute("currentMenuGroup", "admin");
    }
}
