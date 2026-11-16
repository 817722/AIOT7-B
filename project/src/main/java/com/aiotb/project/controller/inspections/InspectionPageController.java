package com.aiotb.project.controller.inspections;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.aiotb.project.service.inspections.InspectionListViewService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class InspectionPageController {
    private static final int PAGE_SIZE = 8;
    private final InspectionListViewService inspectionListViewService;

    @GetMapping("/inspections")
    public String list(
            Authentication authentication,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "year", defaultValue = "") String year,
            @RequestParam(name = "month", defaultValue = "") String month,
            @RequestParam(name = "province", defaultValue = "") String province,
            @RequestParam(name = "city", defaultValue = "") String city,
            @RequestParam(name = "facilityType", defaultValue = "") String facilityType,
            @RequestParam(name = "sort", defaultValue = "nameAsc") String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {

        // 1. 로그인한 기업의 교량 목록 조회
        List<Map<String, Object>> rows = inspectionListViewService.list(authentication.getName());

        // 2. 검색 조건 적용
        List<Map<String, Object>> filtered = rows.stream()
                .filter(item -> {
                    String name = String.valueOf(item.get("name"));
                    String address = String.valueOf(item.get("address"));

                    return keyword.isBlank()
                            || name.contains(keyword)
                            || address.contains(keyword);
                })
                .filter(item -> province.isBlank()
                        || province.equals(item.get("province")))
                .filter(item -> city.isBlank()
                        || city.equals(item.get("city")))
                .filter(item -> facilityType.isBlank()
                        || facilityType.equals(item.get("facilityType")))
                .filter(item -> {
                    LocalDate date = (LocalDate) item.get("lastInspectionDate");

                    if (date == null)
                        return year.isBlank() && month.isBlank();

                    return (year.isBlank()
                            || String.valueOf(date.getYear()).equals(year))
                            && (month.isBlank()
                                    || String.valueOf(date.getMonthValue()).equals(month));
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // 3. 정렬
        Comparator<Map<String, Object>> comparator;

        if ("dateDesc".equals(sort)) {
            comparator = Comparator.comparing(
                    (Map<String, Object> item) -> (LocalDate) item.get("lastInspectionDate"),
                    Comparator.nullsLast(Comparator.reverseOrder()));
        } else {
            comparator = Comparator.comparing(
                    item -> String.valueOf(item.get("name")),
                    java.text.Collator.getInstance(Locale.KOREAN));

            if ("nameDesc".equals(sort)) {
                comparator = comparator.reversed();
            }
        }

        filtered.sort(comparator);

        // 4. 페이지 처리
        int resultCount = filtered.size();
        int totalPages = Math.max(
                1,
                (resultCount + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.max(
                1,
                Math.min(page, totalPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, resultCount);

        List<Map<String, Object>> pageRows = filtered.subList(from, to);

        // 5. Thymeleaf 화면에 데이터 전달
        model.addAttribute("rows", pageRows);
        // 전체 점검 대상 교량 개수
        model.addAttribute("totalCount", rows.size());
        // 검색 조건에 해당하는 전체 결과 개수
        model.addAttribute("resultCount", resultCount);
        // 페이지 정보
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageNo", currentPage);
        // 이전 / 다음 페이지
        model.addAttribute("hasPrev", currentPage > 1);
        model.addAttribute("hasNext", currentPage < totalPages);

        model.addAttribute("keyword", keyword);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("province", province);
        model.addAttribute("city", city);
        model.addAttribute("facilityType", facilityType);
        model.addAttribute("sort", sort);

        model.addAttribute("activeMenu", "inspections");
        // submenu.html은 currentMenuGroup 값으로 메뉴를 분기하므로 반드시 전달합니다.
        model.addAttribute("currentMenuGroup", "inspections");

        return "page/inspections/list";
    }
}