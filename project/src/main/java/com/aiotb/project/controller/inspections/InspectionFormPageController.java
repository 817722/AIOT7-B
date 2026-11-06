package com.aiotb.project.controller.inspections;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping; // ➕ 추가
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.service.inspections.InspectionListViewService;
import com.aiotb.project.service.inspections.InspectionUiWriteService;
import lombok.RequiredArgsConstructor;

/** 점검 등록·기록 작성 화면: 실제 DB 조회와 담당자 변경 이력 연결. */
@Controller
@RequiredArgsConstructor
public class InspectionFormPageController {
    private final InspectionListViewService inspectionListViewService;
    private final InspectionUiWriteService inspectionUiWriteService;

    /** 1. 등록 폼 화면을 열어주는 메서드 (기존 역할로 원복) */
    @GetMapping("/inspections/new")
    public String create(Authentication authentication, Model model) {
        setup(model, false, blank(), isAdmin(authentication));
        return "page/inspections/form";
    }

    /** 2. 🎥 화면에서 [저장] 버튼을 눌렀을 때 메모와 동영상을 전송받는 메서드 (새로 추가) */
    @PostMapping("/inspections/new") // HTML Form의 action 경로와 매핑 (method="post")
    public String save(
        Authentication authentication,
        @RequestParam("keyword") String keyword, // 기존 메모 입력값
        @RequestParam(value = "videoFiles", required = false) List<MultipartFile> videoFiles // 다중 영상 파일 배열
    ) {
        System.out.println("입력된 메모: " + keyword);
        
        if (videoFiles != null && !videoFiles.isEmpty()) {
            for (MultipartFile file : videoFiles) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    System.out.println("업로드된 파일명: " + originalFilename);
                    
                    /* 
                       [비즈니스 로직 작성 공간]
                       예: inspectionUiWriteService를 호출하여 디비에 메모를 저장하고,
                       영상을 특정 폴더나 AWS S3에 저장하는 코드가 이곳에 들어갑니다.
                    */
                }
            }
        }
        
        // 저장이 완료된 후 목록 화면 등으로 리다이렉트
        return "redirect:/inspections"; 
    }

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
        // 서브메뉴 관련
        model.addAttribute("currentMenuGroup", "inspections" );
    }
} // ⚠️ 누락되었던 클래스 닫는 괄호 추가