package com.aiotb.project.controller.bridges;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * 4단계 교량 상세 화면: 데이터 연동 전까지 고정 시연 데이터만 사용.
 * 실제 교량/점검/결함/유지보수/조치 조회는 팀원의 Service 구현 후 이 컨트롤러에서 연결.
 * 이미 운영 중인 BridgePageController, BridgeFormPageController는 변경하지 않는다.
 */
@Controller
public class BridgeDetailPageController {

    // 화면용 BR-2026-xxx 번호는 DDL에 컬럼이 없으며 예시일 뿐입니다.
    private static final List<BridgeView> SAMPLE_BRIDGES = List.of(
            new BridgeView(1L, "BR-2026-001", "가람교", "경기도", "가평군", "제1종",
                    "김가람", "경기도 가평군", "국도 46호선", LocalDate.of(2026, 10, 16), "진행 중"),
            new BridgeView(2L, "BR-2026-002", "나루교", "경기도", "수원시", "제2종",
                    "박나루", "경기도 수원시", "지방도", LocalDate.of(2026, 10, 1), "진행 중"),
            new BridgeView(3L, "BR-2026-003", "다온교", "경기도", "성남시", "제3종",
                    "정다온", "경기도 성남시", "시도", LocalDate.of(2026, 9, 22), "완료"),
            new BridgeView(4L, "BR-2026-004", "라니교", "경기도", "고양시", "제1종",
                    "고라니", "경기도 고양시", "국도", LocalDate.of(2026, 9, 10), "완료"),
            new BridgeView(5L, "BR-2026-005", "마루교", "경기도", "파주시", "제3종",
                    "김마루", "경기도 파주시", "지방도", LocalDate.of(2026, 8, 28), "완료")
    );

    // 컬럼명은 실제 엔티티 속성명과 다를 수 있습니다. 시연 데이터를 표시하기 위한 전용 record.
    public record BridgeView(long no, String displayCode, String name, String province,
            String city, String facilityType, String manager, String address,
            String roadName, LocalDate lastInspectionDate, String inspectionStatus) { }

    public record InspectionRow(String displayCode, String type, LocalDate date,
            String inspector, String status, String summary) { }

    public record DefectRow(String displayCode, String type, String location,
            String severity, String actionStatus) { }

    public record MaintenanceRow(String displayCode, String type, LocalDate date,
            String manager, String status) { }

    public record ActionRow(String displayCode, String action, LocalDate date,
            String manager, String status) { }

    public record RecentRow(String displayCode, String category, LocalDate date,
            String description, String status) { }

    @GetMapping("/bridges/{no}")
    public String detail(@PathVariable("no") Long no, Model model) {
        BridgeView bridge = SAMPLE_BRIDGES.stream()
                .filter(item -> item.no() == no)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 번호의 시연용 교량이 없습니다."));

        List<InspectionRow> inspections = List.of();
        List<DefectRow> defects = List.of();
        List<MaintenanceRow> maintenances = List.of();
        List<ActionRow> actions = List.of();
        List<RecentRow> recents = List.of();

        if (no == 1L) {
            inspections = List.of(
                    new InspectionRow("IN-2026-016", "정기 안전점검", LocalDate.of(2026, 10, 16),
                            "김가람", "진행 중", "교량 상부 점검 중"),
                    new InspectionRow("IN-2026-009", "자체 안전점검", LocalDate.of(2026, 9, 12),
                            "이예슬", "완료", "일상 점검 완료"));
            defects = List.of(
                    new DefectRow("DF-2026-016", "균열", "교량 바닥판", "높음", "조치 중"),
                    new DefectRow("DF-2026-009", "표면 박리", "교대 A1", "보통", "조치 필요"));
            maintenances = List.of(
                    new MaintenanceRow("MT-2026-008", "단면 복구", LocalDate.of(2026, 10, 9),
                            "김가람", "완료"));
            actions = List.of(
                    new ActionRow("FL-2026-004", "균열 보수", LocalDate.of(2026, 10, 17),
                            "김가람", "조치 중"));
            recents = List.of(
                    new RecentRow("FL-2026-004", "후속 조치", LocalDate.of(2026, 10, 17),
                            "교량 바닥판 균열 보수 진행", "조치 중"),
                    new RecentRow("IN-2026-016", "교량 점검", LocalDate.of(2026, 10, 16),
                            "정기 안전점검", "진행 중"),
                    new RecentRow("MT-2026-008", "유지보수", LocalDate.of(2026, 10, 9),
                            "단면 복구", "완료"),
                    new RecentRow("DF-2026-009", "결함 관리", LocalDate.of(2026, 9, 12),
                            "표면 박리 발견", "조치 필요"));
        } else if (no == 2L) {
            inspections = List.of(new InspectionRow("IN-2026-021", "정기 안전점검",
                    LocalDate.of(2026, 10, 1), "박나루", "진행 중", "시설물 상태 확인 중"));
            recents = List.of(new RecentRow("IN-2026-021", "교량 점검",
                    LocalDate.of(2026, 10, 1), "정기 안전점검", "진행 중"));
        }

        model.addAttribute("activeMenu", "bridges");
        model.addAttribute("demoMode", true);
        model.addAttribute("bridge", bridge);
        // DB 대표 이미지는 media/bridge_media 연동 후 채웁니다.
        model.addAttribute("representativeImageUrl", null);
        model.addAttribute("inspections", inspections);
        model.addAttribute("defects", defects);
        model.addAttribute("maintenances", maintenances);
        model.addAttribute("actions", actions);
        model.addAttribute("recents", recents);
        model.addAttribute("inspectionCount", inspections.size());
        model.addAttribute("defectCount", defects.size());
        model.addAttribute("maintenanceCount", maintenances.size());
        model.addAttribute("actionCount", actions.size());
        return "page/bridges/detail";
    }
}
