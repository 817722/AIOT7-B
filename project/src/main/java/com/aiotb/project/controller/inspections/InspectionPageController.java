package com.aiotb.project.controller.inspections;

import com.aiotb.project.domain.BridgeInspectionEntity;
import com.aiotb.project.repository.BridgeInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class InspectionPageController {

    private final BridgeInspectionRepository inspectionRepository;
    private static final int PAGE_SIZE = 5;

    @GetMapping("/inspections")
    public String list(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "status", defaultValue = "") String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {

        String query = keyword.trim();
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));

        // 실제 DB에서 조건에 맞게 조회
        Page<BridgeInspectionEntity> inspectionPage;
        if (StringUtils.hasText(status)) {
            inspectionPage = inspectionRepository.findByKeywordContainingAndStatus(query, status, pageable);
        } else {
            inspectionPage = inspectionRepository.findByKeywordContaining(query, pageable);
        }

        // 상태별 카운트 (HTML의 progressCount, completedCount, pendingCount와 일치)
        long totalCount = inspectionRepository.count();
        long progressCount = inspectionRepository.countByStatus("진행 중");
        long completedCount = inspectionRepository.countByStatus("완료");
        long pendingCount = inspectionRepository.countByStatus("진행 대기");

        int totalPages = Math.max(1, inspectionPage.getTotalPages());
        int currentPage = Math.max(1, Math.min(page, totalPages));

        // HTML 템플릿에 데이터 전달
        model.addAttribute("rows", inspectionPage.getContent());
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("progressCount", progressCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("resultCount", inspectionPage.getTotalElements());

        model.addAttribute("keyword", query);
        model.addAttribute("status", status);
        model.addAttribute("pageNo", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrev", inspectionPage.hasPrevious());
        model.addAttribute("hasNext", inspectionPage.hasNext());

        // 레이아웃 메뉴 활성화 유지
        model.addAttribute("currentMenuGroup", "inspections");
        model.addAttribute("activeMenu", "/inspections/list");

        return "page/inspections/list";
    }
}