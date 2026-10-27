package com.aiotb.project.controller.corps;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiotb.project.service.corps.BusinessVerificationService;

@RestController
@RequestMapping("/api/business")
public class BusinessVerificationController {

    private final BusinessVerificationService verificationService;

    public BusinessVerificationController(
            BusinessVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/verify")
    public Map<?, ?> verify(@RequestBody VerifyRequest request) {
        return verificationService.verify(
                request.businessNo(),
                request.openDate(),
                request.representativeName()
        );
    }

    public record VerifyRequest(
            String businessNo,
            String openDate,
            String representativeName
    ) {}
}