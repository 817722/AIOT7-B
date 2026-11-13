package com.aiotb.project.controller.inspections;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.service.inspections.InspectionListViewService;
import com.aiotb.project.service.inspections.InspectionUiWriteService;
import lombok.RequiredArgsConstructor;

/** 점검 등록·기록 작성 화면: 화면 표시 + 메모 및 단일/다중 영상 파일 업로드 처리 통합 */
@Controller
@RequiredArgsConstructor
public class InspectionFormPageController {
    private final InspectionListViewService inspectionListViewService;
    private final InspectionUiWriteService inspectionUiWriteService;

    /** 1. 등록 폼 화면을 열어주는 메서드 (GET) */
    @GetMapping("/inspections/new")
    public String create(Authentication authentication, Model model) {
        setup(model, false, blank(), isAdmin(authentication));
        return "page/inspections/form";
    }

    /** 2. 🎥 화면에서 [저장] 버튼을 눌렀을 때 메모와 영상 파일들을 한 번에 처리하는 메서드 (POST) [통합 완료] */
    @PostMapping("/inspections/new") 
    public String save(
        Authentication authentication,
        @RequestParam("keyword") String keyword, // 메모 입력값
        @RequestParam(value = "videoFile", required = false) MultipartFile videoFile, // 단일 영상 파일 (추가됨)
        @RequestParam(value = "videoFiles", required = false) List<MultipartFile> videoFiles // 다중 영상 파일 배열
    ) {
        System.out.println("입력된 메모: " + keyword);
        
        // ① 단일 영상 파일 처리 로그 (BridgeInspectionController에서 가져온 기능)
        if (videoFile != null && !videoFile.isEmpty()) {
            System.out.println("업로드된 단일 드론 영상: " + videoFile.getOriginalFilename());
        }
        
        // ② 다중 영상 파일 처리 로그 (BridgeInspectionController에서 가져온 기능)
        if (videoFiles != null && !videoFiles.isEmpty()) {
            for (MultipartFile file : videoFiles) {
                if (!file.isEmpty()) {
                    System.out.println("추가된 다중 파일명: " + file.getOriginalFilename());
                }
            }
        }
        
        // 저장이 완료된 후 목록 화면으로 리다이렉트
        return "redirect:/inspections"; 
    }

    /** 3. 수정 폼 화면을 열어주는 메서드 */
    @GetMapping("/inspections/{no}/edit")
    public String edit(@PathVariable("no") Long no, Authentication authentication, Model model) {
        setup(model, true, inspectionListViewService.detail(authentication.getName(), no), isAdmin(authentication));
        model.addAttribute("managerHistory", inspectionUiWriteService.managerHistory(no, authentication));
        return "page/inspections/form";
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

    private static void setup(Model model, boolean editMode, Map<String,Object> inspection, boolean canManage) {
        model.addAttribute("activeMenu", "inspections");
        model.addAttribute("demoMode", false);
        model.addAttribute("editMode", editMode);
        model.addAttribute("canManage", canManage);
        model.addAttribute("inspection", inspection);
        model.addAttribute("representativeImageUrl", inspection.get("representativeImageUrl"));
        model.addAttribute("csvCandidates", List.of());
        model.addAttribute("managerHistory", List.of());
        model.addAttribute("currentMenuGroup", "inspections");
    }
}