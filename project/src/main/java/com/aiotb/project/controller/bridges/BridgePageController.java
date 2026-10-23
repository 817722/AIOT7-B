package com.aiotb.project.controller.bridges;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 2단계 교량 목록 화면용 Controller.
 * 주의: DB/API가 완성되기 전까지 SAMPLE_BRIDGES 예시 데이터만 사용합니다.
 * 팀원의 교량 조회 Service가 완성되면 이 예시 데이터 부분만 교체하세요.
 */
@Controller
public class BridgePageController {

    private static final int PAGE_SIZE = 5;

    private static final List<Map<String, Object>> SAMPLE_BRIDGES = List.of(
            bridge(1L, "BR-2026-001", "가람교", "경기도", "가평군", "제1종", "김가람", "2026-10-16", "진행 중"),
            bridge(2L, "BR-2026-002", "나루교", "경기도", "수원시", "제2종", "박나루", "2026-10-01", "진행 중"),
            bridge(3L, "BR-2026-003", "다온교", "경기도", "성남시", "제3종", "정다온", "2026-09-22", "완료"),
            bridge(4L, "BR-2026-004", "라니교", "경기도", "고양시", "제1종", "고라니", "2026-09-10", "완료"),
            bridge(5L, "BR-2026-005", "마루교", "경기도", "파주시", "제3종", "김마루", "2026-08-28", "완료")
    );

    private static Map<String, Object> bridge(long no, String code, String name,
            String province, String city, String facilityType, String manager,
            String date, String status) {
        return Map.of(
                "no", no,
                "code", code, // 화면용 *예시* 관리번호: 현행 DDL에는 별도 컬럼 없음
                "name", name,
                "province", province,
                "city", city,
                "facilityType", facilityType,
                "manager", manager,
                "lastInspectionDate", LocalDate.parse(date),
                "status", status
        );
    }

    @GetMapping("/bridges")
    public String list(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "year", defaultValue = "") String year,
            @RequestParam(name = "month", defaultValue = "") String month,
            @RequestParam(name = "province", defaultValue = "") String province,
            @RequestParam(name = "city", defaultValue = "") String city,
            @RequestParam(name = "facilityType", defaultValue = "") String facilityType,
            @RequestParam(name = "sort", defaultValue = "nameAsc") String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {

        String query = keyword.trim();
        Comparator<Map<String, Object>> comparator = Comparator.comparing(
                item -> (String) item.get("name"),
                java.text.Collator.getInstance(Locale.KOREAN)
        );
        if ("nameDesc".equals(sort)) {
            comparator = comparator.reversed();
        } else if ("dateDesc".equals(sort)) {
            comparator = Comparator.comparing(
                    (Map<String, Object> item) -> (LocalDate) item.get("lastInspectionDate")
            ).reversed();
        }

        List<Map<String, Object>> matched = SAMPLE_BRIDGES.stream()
                .filter(item -> query.isBlank()
                        || ((String) item.get("name")).contains(query)
                        || ((String) item.get("code")).contains(query))
                .filter(item -> year.isBlank()
                        || Integer.toString(((LocalDate) item.get("lastInspectionDate")).getYear()).equals(year))
                .filter(item -> month.isBlank()
                        || String.format(Locale.ROOT, "%02d", ((LocalDate) item.get("lastInspectionDate")).getMonthValue()).equals(month))
                .filter(item -> province.isBlank() || item.get("province").equals(province))
                .filter(item -> city.isBlank() || item.get("city").equals(city))
                .filter(item -> facilityType.isBlank() || item.get("facilityType").equals(facilityType))
                .sorted(comparator)
                .toList();

        int total = matched.size();
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        List<Map<String, Object>> rows = matched.subList(from, to);

        long inProgress = SAMPLE_BRIDGES.stream().filter(item -> "진행 중".equals(item.get("status"))).count();
        long completed = SAMPLE_BRIDGES.stream().filter(item -> "완료".equals(item.get("status"))).count();

        List<Integer> yearOptions = SAMPLE_BRIDGES.stream()
                .map(item -> ((LocalDate) item.get("lastInspectionDate")).getYear())
                .distinct().sorted(Comparator.reverseOrder()).toList();
        List<String> monthOptions = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> String.format(Locale.ROOT, "%02d", m)).toList();
        List<String> provinces = SAMPLE_BRIDGES.stream()
                .map(item -> (String) item.get("province")).distinct().sorted().toList();
        List<Map<String, String>> cities = new ArrayList<>();
        SAMPLE_BRIDGES.forEach(item -> {
            Map<String, String> value = Map.of(
                    "province", (String) item.get("province"),
                    "city", (String) item.get("city")
            );
            if (!cities.contains(value)) cities.add(value);
        });
        cities.sort(Comparator.comparing(item -> item.get("city")));

        model.addAttribute("activeMenu", "bridges");
        model.addAttribute("demoMode", true);
        model.addAttribute("rows", rows);
        model.addAttribute("totalCount", SAMPLE_BRIDGES.size());
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("completedCount", completed);
        model.addAttribute("resultCount", total);
        model.addAttribute("pageNo", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrev", currentPage > 1);
        model.addAttribute("hasNext", currentPage < totalPages);
        model.addAttribute("showFrom", total == 0 ? 0 : from + 1);
        model.addAttribute("showTo", to);

        model.addAttribute("keyword", query);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("province", province);
        model.addAttribute("city", city);
        model.addAttribute("facilityType", facilityType);
        model.addAttribute("sort", sort);
        model.addAttribute("yearOptions", yearOptions);
        model.addAttribute("monthOptions", monthOptions);
        model.addAttribute("provinceOptions", provinces);
        model.addAttribute("cityOptions", cities);

        return "page/bridges/list";
    }
}
