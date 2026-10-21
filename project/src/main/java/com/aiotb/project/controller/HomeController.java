package com.aiotb.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.aiotb.project.service.users.UserService;

import lombok.RequiredArgsConstructor;


@Controller 
@RequiredArgsConstructor 
public class HomeController {

    private final UserService userService;

    @GetMapping("/")
    public String home(Model model) {
        return "index";
    }
    

}
