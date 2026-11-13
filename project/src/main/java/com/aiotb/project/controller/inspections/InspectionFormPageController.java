package com.aiotb.project.controller.inspections;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.dto.inspections.InspectionDto;
import com.aiotb.project.service.inspections.InspectionListViewService;
import com.aiotb.project.service.inspections.InspectionService;
import com.aiotb.project.service.inspections.InspectionUiWriteService;
import com.aiotb.project.service.inspections.InspectionVideoService;
import com.aiotb.project.service.users.CurrentUserService;

import lombok.RequiredArgsConstructor;

/** 점검 등록·기록 작성 화면: 화면 표시 + 메모 및 단일/다중 영상 파일 업로드 처리 통합 */
@Controller
@RequiredArgsConstructor
public class InspectionFormPageController {
    private final InspectionListViewService inspectionListViewService;
    private final InspectionUiWriteService inspectionUiWriteService;
    private final InspectionService inspectionService;
    private final InspectionVideoService inspectionVideoService;
    private final CurrentUserService currentUserService;

    /** 1. 등록 폼 화면을 열어주는 메서드 (GET) */
    @GetMapping("/inspections/new")
    public String create(Authentication authentication, Model model) {
        setup(model, false, blank(), isAdmin(authentication));
        return "page/inspections/form";
    }

    /**
     * 2. 신규 점검 기록 생성 + 드론 영상 업로드
     */
    @PostMapping(value = "/inspections/new", consumes = "multipart/form-data")
    public String saveNewInspection(
            Authentication authentication,
            @ModelAttribute InspectionDto.Request request) {

        // 로그인 사용자 확인
        var actor = currentUserService.current(authentication);
        // 점검 기록 생성 + 단일/다중 영상 저장
        inspectionService.createInspection(request, actor);

        return "redirect:/inspections";
    }

    /**
     * 3. 기존 점검 기록에 드론 영상 추가
     */
    @PostMapping(value = "/inspections/{inspectionNo}/media", consumes = "multipart/form-data")
    public String addInspectionVideos(
            @PathVariable Long inspectionNo,
            Authentication authentication,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
            @RequestParam(value = "videoFiles", required = false) List<MultipartFile> videoFiles) {

        // 로그인 사용자 확인
        var actor = currentUserService.current(authentication);
        // 단일 및 다중 파일 통합
        List<MultipartFile> files = new ArrayList<>();

        if (videoFile != null && !videoFile.isEmpty()) {
            files.add(videoFile);
        }
        if (videoFiles != null) {
            videoFiles.stream()
                    .filter(file -> file != null && !file.isEmpty())
                    .forEach(files::add);
        }

        // 업로드할 파일이 없는 경우
        if (files.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "업로드할 영상 파일을 선택해 주세요.");
        }

        // 기존 점검 기록에 영상 저장
        inspectionVideoService.saveInspectionVideos(
                inspectionNo,
                files,
                actor);
        return "redirect:/inspections/" + inspectionNo;
    }

    /** 4. 수정 폼 화면을 열어주는 메서드 */
    @GetMapping("/inspections/{no}/edit")
    public String edit(@PathVariable("no") Long no, Authentication authentication, Model model) {
        setup(model, true, inspectionListViewService.detail(authentication.getName(), no), isAdmin(authentication));
        model.addAttribute("managerHistory", inspectionUiWriteService.managerHistory(no, authentication));
        return "page/inspections/form";
    }

    private static Map<String, Object> blank() {
        Map<String, Object> row = new LinkedHashMap<>();
        for (String key : List.of("no", "code", "name", "province", "city", "facilityType", "manager", "managerNo",
                "address", "roadName")) {
            row.put(key, "");
        }
        return row;
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private static void setup(Model model, boolean editMode, Map<String, Object> inspection, boolean canManage) {
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