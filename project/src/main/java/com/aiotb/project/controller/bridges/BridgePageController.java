package com.aiotb.project.controller.bridges;

import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.aiotb.project.service.bridges.BridgeListViewService;
import lombok.RequiredArgsConstructor;

/** 최신 DDL 기반 기업별 교량 목록 화면. 기존 샘플 목록 Controller를 교체합니다. */
@Controller
@RequiredArgsConstructor
public class BridgePageController {
    private static final int PAGE_SIZE = 8;
    private final BridgeListViewService bridgeListViewService;

    @GetMapping("/bridges")
    public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "year", defaultValue = "") String year,
                       @RequestParam(name = "month", defaultValue = "") String month,
                       @RequestParam(name = "province", defaultValue = "") String province,
                       @RequestParam(name = "city", defaultValue = "") String city,
                       @RequestParam(name = "facilityType", defaultValue = "") String facilityType,
                       @RequestParam(name = "sort", defaultValue = "nameAsc") String sort,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       Authentication authentication, Model model) {
        List<Map<String,Object>> all = bridgeListViewService.list(authentication.getName());
        String query = keyword.trim();
        List<Map<String,Object>> matched = all.stream()
            .filter(item -> query.isBlank()
                || ((String)item.get("name")).contains(query)
                || ((String)item.get("code")).contains(query))
            .filter(item -> year.isBlank() || (item.get("lastInspectionDate") instanceof LocalDate d
                && String.valueOf(d.getYear()).equals(year)))
            .filter(item -> month.isBlank() || (item.get("lastInspectionDate") instanceof LocalDate d
                && String.format(Locale.ROOT, "%02d", d.getMonthValue()).equals(month)))
            .filter(item -> province.isBlank() || province.equals(item.get("province")))
            .filter(item -> city.isBlank() || city.equals(item.get("city")))
            .filter(item -> facilityType.isBlank() || facilityType.equals(item.get("facilityType")))
            .sorted(comparator(sort)).toList();
        int resultCount = matched.size();
        int pages = Math.max(1, (resultCount + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.max(1, Math.min(page, pages));
        int from = (current - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, resultCount);
        List<Integer> years = all.stream()
            .map(row -> (LocalDate)row.get("lastInspectionDate"))
            .filter(java.util.Objects::nonNull).map(LocalDate::getYear)
            .distinct().sorted(Comparator.reverseOrder()).toList();
        List<String> provinces = all.stream().map(row -> (String)row.get("province"))
            .filter(s -> !s.isBlank()).distinct().sorted().toList();
        List<Map<String,String>> cities = new ArrayList<>();
        all.forEach(row -> {
            String p = (String)row.get("province"), c = (String)row.get("city");
            Map<String,String> pc = Map.of("province", p, "city", c);
            if (!c.isBlank() && !cities.contains(pc)) cities.add(pc);
        });
        cities.sort(Comparator.comparing(row -> row.get("city")));
        model.addAttribute("activeMenu", "bridges");
        model.addAttribute("demoMode", false);
        model.addAttribute("rows", matched.subList(from, to));
        model.addAttribute("totalCount", all.size());
        model.addAttribute("resultCount", resultCount);
        model.addAttribute("inProgressCount", all.stream().filter(row -> "진행 중".equals(row.get("status"))).count());
        model.addAttribute("completedCount", all.stream().filter(row -> "완료".equals(row.get("status"))).count());
        model.addAttribute("pageNo", current);
        model.addAttribute("totalPages", pages);
        model.addAttribute("hasPrev", current > 1);
        model.addAttribute("hasNext", current < pages);
        model.addAttribute("showFrom", resultCount == 0 ? 0 : from + 1);
        model.addAttribute("showTo", to);
        model.addAttribute("keyword", query);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("province", province);
        model.addAttribute("city", city);
        model.addAttribute("facilityType", facilityType);
        model.addAttribute("sort", sort);
        model.addAttribute("yearOptions", years);
        model.addAttribute("monthOptions", IntStream.rangeClosed(1,12)
            .mapToObj(m -> String.format(Locale.ROOT, "%02d", m)).toList());
        model.addAttribute("provinceOptions", provinces);
        model.addAttribute("cityOptions", cities);
        return "page/bridges/list";
    }

    private static Comparator<Map<String,Object>> comparator(String sort) {
        Comparator<Map<String,Object>> byName = Comparator.comparing(
            row -> (String)row.get("name"), Collator.getInstance(Locale.KOREAN));
        if ("nameDesc".equals(sort)) return byName.reversed();
        if ("dateDesc".equals(sort)) return Comparator.comparing(
            (Map<String,Object> row) -> (LocalDate)row.get("lastInspectionDate"),
            Comparator.nullsLast(Comparator.reverseOrder()));
        return byName;
    }
}
