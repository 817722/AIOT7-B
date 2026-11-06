package com.aiotb.project.controller.maintenances;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.aiotb.project.service.maintenance.MaintenanceListViewService;
import com.aiotb.project.service.maintenance.MaintenanceUiWriteService;

import lombok.RequiredArgsConstructor;

/** 유지보수 등록·수정 화면. 이전 샘플 교량 조회를 실제 기업별 DB 조회로 전환. */
@Controller
@RequiredArgsConstructor
public class MaintenanceFormPageController {
    private final MaintenanceListViewService viewService;
    private final MaintenanceUiWriteService writeService;

    @GetMapping("/maintenances/new")
    public String create(@RequestParam(name = "bridgeNo", required = false) Long bridgeNo,
                         Authentication authentication, Model model) {
        // 로그인 검증과 같은 기업 사용자 검증은 writeService에서 수행합니다.
        List<Map<String, Object>> managers = writeService.managers(authentication);
        Map<String, Object> selectedBridge = (bridgeNo != null && bridgeNo > 0)
                ? writeService.bridge(authentication, bridgeNo) : null;
        common(model, managers);
        model.addAttribute("editMode", false);
        model.addAttribute("canEdit", true);
        model.addAttribute("maintenance", null);
        model.addAttribute("selectedBridge", selectedBridge);
        model.addAttribute("referenceInspections", selectedBridge == null ? List.of()
                : writeService.inspections(authentication, bridgeNo));
        return "page/maintenances/form";
    }

    @GetMapping("/maintenances/{no}/edit")
    public String edit(@PathVariable("no") long no,
                       Authentication authentication, Model model) {
        String username = authentication == null ? null : authentication.getName();
        Map<String, Object> maintenance = viewService.detail(username, no);
        boolean canEdit = writeService.canEdit(authentication, no);
        if (!canEdit) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 기록을 수정할 권한이 없습니다.");
        }
        Long bridgeNo = ((Number) maintenance.get("bridgeNo")).longValue();
        Map<String, Object> bridge = writeService.bridge(authentication, bridgeNo);
        common(model, writeService.managers(authentication));
        model.addAttribute("editMode", true);
        model.addAttribute("canEdit", true);
        model.addAttribute("maintenance", maintenance);
        model.addAttribute("selectedBridge", bridge);
        model.addAttribute("referenceInspections", writeService.inspections(authentication, bridgeNo));
        return "page/maintenances/form";
    }

    private static void common(Model model, List<Map<String, Object>> managers) {
        model.addAttribute("managers", managers);
        model.addAttribute("currentMenuGroup", "maintenances");
        model.addAttribute("activeMenu", "maintenances");
    }
}
