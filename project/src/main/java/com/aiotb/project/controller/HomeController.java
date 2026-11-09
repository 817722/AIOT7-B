package com.aiotb.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * 기존 "/{page}" 매핑은 URL 값을 그대로 뷰 이름으로 사용해
     * 임의 템플릿 렌더링(템플릿 인젝션) 위험이 있었고,
     * /favicon.ico 요청도 "favicon.ico" 뷰를 찾다가 500 오류를 냈으므로 제거했다.
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}
