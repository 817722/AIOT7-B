package com.aiotb.project.controller.bridges;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;


/**
 * 교량 등록·수정 화면(3단계) 컨트롤러.
 * 현재 데이터는 화면 테스트 전용이며 DB 저장/수정/삭제 기능이 없습니다.
 * 교량 백엔드가 완성되면 SAMPLE_CSV와 findSample()을 실제 Service 조회로 교체해야 합니다.
 */
@Controller 
public class BridgeFormPageController {

    private static final List<Map<String, String>> SAMPLE_CSV = List.of(
        sample("1", "BR-2026-001", "가람교", "경기도", "가평군", "제1종", "김가람", "경기도 가평군", "국도 46호선"),
        sample("2", "BR-2026-002", "나루교", "경기도", "수원시", "제2종", "박나루", "경기도 수원시", "지방도"),
        sample("3", "BR-2026-003", "다온교", "경기도", "성남시", "제3종", "정다온", "경기도 성남시", "시도"),
        sample("4", "BR-2026-004", "라니교", "경기도", "고양시", "제1종", "고라니", "경기도 고양시", "국도"),
        sample("5", "BR-2026-005", "마루교", "경기도", "파주시", "제3종", "김마루", "경기도 파주시", "지방도")
    );

    private static Map<String, String> sample(String no, String code, String name,
        String province, String city, String facilityType, String manager,
        String address, String roadName) {
            return Map.of(
                "no", no,
                "code", code,
                "name", name,
                "province", province,
                "city", city,
                "facilityType", facilityType,
                "manager", manager,
                "address", address,
                "roadName", roadName
            );
        }

        @GetMapping("/bridges/new")
        public String create(Model model) {
            setup(model, false, Map.of());
            return "page/bridges/form";
        }

        @GetMapping("/bridges/{no}/edit")
        public String edit(@PathVariable("no") Long no, Model model) {
            Map<String, String> bridge = SAMPLE_CSV.stream()
                .filter(item -> item.get("no").equals(String.valueOf(no)))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "테스트용 목록에 없는 교량입니다."));
            setup(model, true, bridge);
            return "page/bridges/form";
        }

        private void setup(Model model, boolean editMode, Map<String, String> bridge) {
            model.addAttribute("activeMenu", "bridges");
            model.addAttribute("demoMode", true);
            model.addAttribute("editMode", editMode);
            model.addAttribute("bridge", bridge);
            model.addAttribute("csvCandidates", SAMPLE_CSV);
        }
        
        
}
