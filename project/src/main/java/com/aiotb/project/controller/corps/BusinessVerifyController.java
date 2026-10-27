package com.aiotb.project.controller.corps;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiotb.project.dto.corps.BusinessVerifyDto;
import com.aiotb.project.service.corps.BusinessVerifyService;

@RestController
@RequestMapping("/api/business")
public class BusinessVerifyController {

    private final BusinessVerifyService businessVerifyService;

    // MockBusinessVerifyServiceImpl 주입
    public BusinessVerifyController(BusinessVerifyService businessVerifyService) {
        this.businessVerifyService = businessVerifyService;
    }

    @PostMapping("/verify")
    public BusinessVerifyDto.Response verify(@RequestBody BusinessVerifyDto.Request request) {
        return businessVerifyService.verifyBusiness(request);
    }
}
