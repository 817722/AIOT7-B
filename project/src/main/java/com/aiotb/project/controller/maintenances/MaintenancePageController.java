package com.aiotb.project.controller.maintenances;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.aiotb.project.service.bridges.BridgeRecentViewService;
import com.aiotb.project.service.maintenance.MaintenanceListViewService;

import lombok.RequiredArgsConstructor;

/** Figma 유지보수 목록: 로그인 기업의 실제 유지보수 기록과 최근 교량 5건 표시. */
@Controller
@RequiredArgsConstructor
public class MaintenancePageController {
    private static final int PAGE_SIZE = 8;
    private final MaintenanceListViewService maintenanceViewService;
    private final BridgeRecentViewService bridgeRecentViewService;

    @GetMapping("/maintenances")
    public String list(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "managerNo", required = false) Long managerNo,
            @RequestParam(name = "status", defaultValue = "") String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Authentication authentication, Model model) {
        String username = authentication == null ? null : authentication.getName();
        List<Map<String, Object>> all = maintenanceViewService.list(username);
        String search = keyword == null ? "" : keyword.trim();
        String safeStatus = List.of("진행 대기", "진행 중", "완료").contains(status) ? status : "";
        if (managerNo != null && managerNo <= 0) managerNo = null;
        final Long selectedManagerNo = managerNo;

        List<Map<String, Object>> matched = all.stream()
            .filter(row -> search.isBlank() || ((String) row.get("name")).contains(search)
                    || ((String) row.get("title")).contains(search)
                    || String.valueOf(row.get("bridgeNo")).equals(search))
            .filter(row -> startDate == null || (row.get("maintenanceDate") instanceof LocalDate d
                    && !d.isBefore(startDate)))
            .filter(row -> endDate == null || (row.get("maintenanceDate") instanceof LocalDate d
                    && !d.isAfter(endDate)))
            .filter(row -> selectedManagerNo == null || Objects.equals(row.get("managerNo"), selectedManagerNo))
            .filter(row -> safeStatus.isBlank() || safeStatus.equals(row.get("status")))
            .sorted(Comparator.comparing((Map<String, Object> row) ->
                    (LocalDate) row.get("maintenanceDate"),
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(row -> (Long) row.get("no"), Comparator.reverseOrder()))
            .toList();

        int resultCount = matched.size();
        int totalPages = Math.max(1, (resultCount + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, resultCount);

        model.addAttribute("activeMenu", "maintenances");
        model.addAttribute("currentMenuGroup", "maintenances");
        model.addAttribute("recentBridges", bridgeRecentViewService.listRecent(username));
        model.addAttribute("rows", matched.subList(from, to));
        model.addAttribute("totalCount", all.size());
        model.addAttribute("progressCount", all.stream().filter(r -> "진행 중".equals(r.get("status"))).count());
        model.addAttribute("pendingCount", all.stream().filter(r -> "진행 대기".equals(r.get("status"))).count());
        model.addAttribute("completedCount", all.stream().filter(r -> "완료".equals(r.get("status"))).count());
        model.addAttribute("resultCount", resultCount);
        model.addAttribute("pageNo", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrev", currentPage > 1);
        model.addAttribute("hasNext", currentPage < totalPages);
        model.addAttribute("keyword", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("managerNo", selectedManagerNo);
        model.addAttribute("status", safeStatus);
        model.addAttribute("managers", maintenanceViewService.managers(username));
        return "page/maintenances/list";
    }
}
