package com.aiotb.project.controller.bridges;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.aiotb.project.service.bridges.BridgeListViewService;
import lombok.RequiredArgsConstructor;

/** 실제 DB 기반의 읽기 전용 상세 페이지. 수정/이미지 업로드 기능은 아직 연동하지 않습니다. */
@Controller
@RequiredArgsConstructor
public class BridgeDetailPageController {
    private final BridgeListViewService bridgeListViewService;

    @GetMapping("/bridges/{no}")
    public String detail(@PathVariable("no") Long no, Authentication authentication, Model model) {
        String username = authentication.getName();
        Map<String,Object> bridge = bridgeListViewService.detail(username, no);
        List<Map<String,Object>> inspections = bridgeListViewService.inspections(username, no);
        List<Map<String,Object>> maintenances = bridgeListViewService.maintenances(username, no);
        model.addAttribute("activeMenu", "bridges");
        model.addAttribute("demoMode", false);
        model.addAttribute("bridge", bridge);
        model.addAttribute("representativeImageUrl", bridge.get("representativeImageUrl"));
        model.addAttribute("inspections", inspections);
        model.addAttribute("maintenances", maintenances);
        model.addAttribute("recents", bridgeListViewService.recentWork(inspections, maintenances));
        model.addAttribute("inspectionCount", inspections.size());
        model.addAttribute("maintenanceCount", maintenances.size());
        return "page/bridges/detail";
    }
}
