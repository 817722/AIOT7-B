package com.aiotb.project.controller.bridges;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.aiotb.project.service.bridges.BridgeListViewService;
import lombok.RequiredArgsConstructor;

/** 실제 CSV API 검색 + 기존 교량의 DB 값 확인. 저장 API 연동 전까지 입력은 읽기 전용. */
@Controller
@RequiredArgsConstructor
public class BridgeFormPageController {
    private final BridgeListViewService bridgeListViewService;

    @GetMapping("/bridges/new")
    public String create(Authentication authentication, Model model) {
        setup(model, false, blank(), isAdmin(authentication));
        return "page/bridges/form";
    }

    @GetMapping("/bridges/{no}/edit")
    public String edit(@PathVariable("no") Long no, Authentication authentication, Model model) {
        setup(model, true, bridgeListViewService.detail(authentication.getName(), no), isAdmin(authentication));
        return "page/bridges/form";
    }

    private static Map<String,Object> blank() {
        Map<String,Object> row = new LinkedHashMap<>();
        for (String key : List.of("no", "code", "name", "province", "city", "facilityType", "manager", "managerNo", "address", "roadName")) {
            row.put(key, "");
        }
        return row;
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream().anyMatch(authority ->
            "ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private static void setup(Model model, boolean editMode, Map<String,Object> bridge, boolean canManage) {
        model.addAttribute("activeMenu", "bridges");
        model.addAttribute("demoMode", false);
        model.addAttribute("editMode", editMode);
        model.addAttribute("canManage", canManage);
        model.addAttribute("bridge", bridge);
        model.addAttribute("representativeImageUrl", bridge.get("representativeImageUrl"));
        model.addAttribute("csvCandidates", List.of());
    }
}
