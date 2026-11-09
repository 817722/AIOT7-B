package com.aiotb.project.controller.maintenances;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.aiotb.project.service.maintenance.MaintenanceListViewService;
import com.aiotb.project.service.maintenance.MaintenanceUiWriteService;

import lombok.RequiredArgsConstructor;

/** 유지보수 상세: 실데이터, 관련 점검 참고 목록, 권한별 영상 UI. */
@Controller
@RequiredArgsConstructor
public class MaintenanceDetailPageController {
    private final MaintenanceListViewService maintenanceViewService;
    private final MaintenanceUiWriteService maintenanceWriteService;

    @GetMapping("/maintenances/{no}")
    public String detail(@PathVariable("no") long no, Authentication authentication, Model model) {
        String username = authentication == null ? null : authentication.getName();
        var maintenance = maintenanceViewService.detail(username, no);
        model.addAttribute("maintenance", maintenance);
        long bridgeNo = ((Number) maintenance.get("bridgeNo")).longValue();
        model.addAttribute("referenceInspections", maintenanceWriteService.inspections(authentication, bridgeNo));
        model.addAttribute("videos", maintenanceViewService.videoMetadata(username, no));
        model.addAttribute("canEdit", maintenanceWriteService.canEdit(authentication, no));
        model.addAttribute("canDelete", maintenanceWriteService.canDelete(authentication, no));
        model.addAttribute("activeMenu", "maintenances");
        model.addAttribute("currentMenuGroup", "maintenances");
        return "page/maintenances/detail";
    }
}
