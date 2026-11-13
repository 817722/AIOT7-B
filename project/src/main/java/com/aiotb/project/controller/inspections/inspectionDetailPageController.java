package com.aiotb.project.controller.inspections;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.aiotb.project.service.inspections.InspectionListViewService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class inspectionDetailPageController {

    private final InspectionListViewService inspectionListViewService;

    /**
     * 교량별 상세 조회
     */
    @GetMapping("/inspections/{no}")
    public String detail(
            @PathVariable("no") Long no,
            Authentication authentication,
            Model model) {

        String username = authentication.getName();

        // 1. 교량 상세 정보
        Map<String, Object> bridge =
                inspectionListViewService.detail(username, no);

        // 2. 해당 교량의 점검 기록
        List<Map<String, Object>> inspections =
                inspectionListViewService.inspections(username, no);

        // 3. 해당 교량의 유지보수 기록
        List<Map<String, Object>> maintenances =
                inspectionListViewService.maintenances(username, no);

        // 4. 최근 작업 내역
        List<Map<String, Object>> recentWork =
                inspectionListViewService.recentWork(
                        inspections, maintenances);

        // 5. 화면 전달
        model.addAttribute("bridge", bridge);
        model.addAttribute("inspections", inspections);
        model.addAttribute("maintenances", maintenances);
        model.addAttribute("recentWork", recentWork);

        model.addAttribute("activeMenu", "inspections");

        return "page/inspections/detail";
    }
}