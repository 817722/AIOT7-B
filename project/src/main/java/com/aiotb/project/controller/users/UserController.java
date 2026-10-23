package com.aiotb.project.controller.users;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aiotb.project.dto.users.UserJoinDto;
import com.aiotb.project.service.users.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입 화면
    // @ModelAttribute : 해당 파라미터를 모델로 자동 지정
    @GetMapping("/join")
    public String join(@ModelAttribute("joinDto") UserJoinDto joinDto) {

        // page/users/join.html 뷰 지정
        return "page/users/join";

    }

    // 회원가입 처리
    // - @Valid : 해당 객체의 유효성을 검증
    // -
    @PostMapping("/join")
    public String join(@Valid @ModelAttribute("joinDto") UserJoinDto joinDto,
            BindingResult bindingResult,
            RedirectAttributes ra) {
        // 유효성 검사 실패
        if (bindingResult.hasErrors()) {
            return "page/users/join";
        }
        // 아이디 중복 확인
        if (!userService.isUsernameAvailable(joinDto.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "이미 사용 중인 아이디입니다.");
            return "page/users/join";
        }
        userService.join(joinDto);
        ra.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해 주세요.");
        return "redirect:/login";
    }

    // 로그인 화면
    @GetMapping("/login")
    public String login() {
        return "page/users/login";
    }

}
