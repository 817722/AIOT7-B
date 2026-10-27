package com.aiotb.project.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.service.users.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller 
@RequiredArgsConstructor 
public class HomeController {

    private final UserService userService;

    @GetMapping({"/", "/{page}"})   //page추가
    public String home(
        @PathVariable(value = "page", required = false) String page,
        Model model,
        HttpSession session,
        User selectBox
    ) {

        //컬렉션
        List<String> items1 = List.of("IN-2026-011", "IN-2026-012", "IN-2026-013", "IN-2026-014", "IN-2026-015");
        model.addAttribute("items1", items1);

        List<String> items2 = List.of("IN-2026-016", "IN-2026-017");
        model.addAttribute("items2", items2);

        List<String> items3 = List.of("IN-2026-018", "IN-2026-019", "IN-2026-020");
        model.addAttribute("items3", items3);

        List<User> user = List.of(
                                        User.builder().username("user1").managerName("사용자1").build(),
                                        User.builder().username("user2").managerName("사용자2").build(),
                                        User.builder().username("user3").managerName("사용자3").build(),
                                        User.builder().username("user4").managerName("사용자4").build(),
                                        User.builder().username("user5").managerName("사용자5").build()
        );
        model.addAttribute("user", user);

        //Select Box - 담당자명
        selectBox.setManagerName("담당자");
        selectBox.setManagerName("김가람");
        selectBox.setManagerName("이나루");

        session.setAttribute("selectBox", selectBox);
        model.addAttribute("selectBox", selectBox);

        //뷰 지정
        return page == null ? "index" : page;
    }
    

}
