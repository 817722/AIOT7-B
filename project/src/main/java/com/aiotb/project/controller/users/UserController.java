package com.aiotb.project.controller.users;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aiotb.project.dto.corps.VerifiedBusiness;
import com.aiotb.project.dto.users.UserJoinDto;
import com.aiotb.project.service.users.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/join")
    public String join(@ModelAttribute("joinDto") UserJoinDto joinDto,
                       HttpSession session, Model model) {
        model.addAttribute("verifiedBusiness", currentVerification(session));
        return "page/users/join";
    }

    @PostMapping("/join")
    public String join(@Valid @ModelAttribute("joinDto") UserJoinDto joinDto,
                       BindingResult bindingResult, HttpSession session,
                       Model model, RedirectAttributes redirectAttributes) {
        VerifiedBusiness verified = currentVerification(session);
        model.addAttribute("verifiedBusiness", verified);
        if (verified == null) {
            model.addAttribute("serverError", "기업 인증이 없거나 만료됐습니다. 다시 인증해 주세요.");
            return "page/users/join";
        }
        model.addAttribute("resumeStep2", true);
        if (joinDto.getPassword() != null && joinDto.getPasswordConfirm() != null
                && !joinDto.getPassword().equals(joinDto.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        }
        if (joinDto.getUsername() != null && !joinDto.getUsername().isBlank()
                && !userService.isUsernameAvailable(joinDto.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "이미 사용 중인 아이디입니다.");
        }
        if (bindingResult.hasErrors()) {
            return "page/users/join";
        }
        try {
            userService.join(joinDto, verified);
        } catch (IllegalArgumentException e) {
            model.addAttribute("serverError", e.getMessage());
            return "page/users/join";
        } catch (DataIntegrityViolationException e) {
            log.warn("가입 DB 제약 조건 충돌", e);
            model.addAttribute("serverError", "이미 등록된 기업 또는 계정일 수 있습니다. 입력 정보를 확인해 주세요.");
            return "page/users/join";
        } catch (Exception e) {
            log.error("가입 처리 중 예기치 못한 오류", e);
            model.addAttribute("serverError", "가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
            return "page/users/join";
        }
        session.removeAttribute(VerifiedBusiness.SESSION_KEY);
        redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해 주세요.");
        return "redirect:/login";
    }

    private VerifiedBusiness currentVerification(HttpSession session) {
        Object value = session.getAttribute(VerifiedBusiness.SESSION_KEY);
        if (value instanceof VerifiedBusiness verified && !verified.expired()) {
            return verified;
        }
        session.removeAttribute(VerifiedBusiness.SESSION_KEY);
        return null;
    }

    @GetMapping("/login")
    public String login() {
        return "page/users/login";
    }
}
