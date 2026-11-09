package com.aiotb.project.controller.corps;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aiotb.project.service.corps.CorpManageService;
import com.aiotb.project.service.corps.CorpManageService.MemberAction;
import com.aiotb.project.service.users.CurrentUserService;

import lombok.RequiredArgsConstructor;

/**
 * 기업 관리자(ADMIN) 페이지: 소속 기업 정보, 담당자 가입 승인·반려·비활성·재활성.
 * 접근 제한: SecurityConfig(/corps/** → ADMIN) + @PreAuthorize + CorpManageService(소속 기업·INSPECTOR 대상 검사)
 */
@Controller
@RequestMapping("/corps")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CorpManagePageController {
    private final CorpManageService corpManageService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String info(Model model) {
        CurrentUserService.Actor actor = currentUserService.current();
        model.addAttribute("corp", corpManageService.corp(actor));
        model.addAttribute("pendingMembers", corpManageService.members(actor, "PENDING", ""));
        menu(model);
        return "page/corps/info";
    }

    @GetMapping("/members")
    public String members(@RequestParam(name = "status", defaultValue = "") String status,
                          @RequestParam(name = "keyword", defaultValue = "") String keyword,
                          Model model) {
        CurrentUserService.Actor actor = currentUserService.current();
        model.addAttribute("members", corpManageService.members(actor, status, keyword));
        model.addAttribute("counts", corpManageService.memberCounts(actor));
        model.addAttribute("status", switch (status) {
            case "PENDING", "ACTIVE", "REJECTED", "DISABLED" -> status;
            default -> "";
        });
        model.addAttribute("keyword", keyword.strip());
        menu(model);
        return "page/corps/members";
    }

    @GetMapping("/members/{no}")
    public String member(@PathVariable("no") long no, Model model) {
        model.addAttribute("member", corpManageService.member(currentUserService.current(), no));
        menu(model);
        return "page/corps/member-detail";
    }

    /** 담당자 상태 처리 (POST + CSRF). returnTo 는 정해진 화면으로만 이동(오픈 리다이렉트 방지) */
    @PostMapping("/members/{no}/status")
    public String changeMemberStatus(@PathVariable("no") long no,
                                     @RequestParam("action") MemberAction action,
                                     @RequestParam(name = "reason", required = false) String reason,
                                     @RequestParam(name = "returnTo", defaultValue = "detail") String returnTo,
                                     RedirectAttributes redirect) {
        try {
            corpManageService.changeMemberStatus(currentUserService.current(), no, action, reason);
            redirect.addFlashAttribute("adminMessage", switch (action) {
                case APPROVE -> "담당자 가입을 승인했습니다. 해당 담당자가 로그인할 수 있습니다.";
                case REJECT -> "담당자 가입 요청을 반려했습니다.";
                case DISABLE -> "담당자 계정을 비활성화했습니다.";
                case REACTIVATE -> "담당자 계정을 다시 활성화했습니다.";
            });
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("adminError", e.getMessage());
        }
        return switch (returnTo) {
            case "list" -> "redirect:/corps/members";
            case "info" -> "redirect:/corps";
            default -> "redirect:/corps/members/" + no;
        };
    }

    private static void menu(Model model) {
        model.addAttribute("activeMenu", "corps");
        model.addAttribute("currentMenuGroup", "corps");
    }
}
