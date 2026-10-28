package com.aiotb.project.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.service.bridges.BridgeService;
import com.aiotb.project.service.inspections.InspectionService;
import com.aiotb.project.service.maintenance.MaintenanceService;
import com.aiotb.project.service.users.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 홈 컨트롤러
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;

    @GetMapping({ "/", "/{page}" }) // page추가
    public String home(
            @PathVariable(value = "page", required = false) String page,
            Model model) {

        return page == null ? "index" : page;
    }

    // 교량 컨트롤러
    @Controller
    @RequiredArgsConstructor
    public class BridgeController {
        private final BridgeService bridgeService;

        @GetMapping({ "/bridge" }) // page추가
        public String bridge(
                @PathVariable(value = "page", required = false) String page,
                Model model) {
            model.addAttribute("currentMenu", "bridge");

            // 뷰 지정
            return "bridge";
        }
    }

    // 교량2 컨트롤러
    @Controller
    @RequiredArgsConstructor
    public class Bridge2Controller {
        private final BridgeService bridgeService;

        @GetMapping({ "/bridge2" }) // page추가
        public String bridge2(
                @PathVariable(value = "page", required = false) String page,
                Model model) {
            model.addAttribute("currentMenu", "bridge2");

            // 뷰 지정
            return "bridge2";
        }
    }

    // 점검 컨트롤러
    @Controller
    @RequiredArgsConstructor
    public class InspectionController {
        private final InspectionService inspectionService;

    @GetMapping({"/inspection"})
    public String inspection(
        @PathVariable(value = "page", required = false) String page,
        Model model,
        HttpSession session,
        User selectBox
    ) {
        model.addAttribute("currentMenu", "inspection");

            // 컬렉션
            List<String> items1 = List.of("IN-2026-011", "IN-2026-012", "IN-2026-013", "IN-2026-014", "IN-2026-015");
            model.addAttribute("items1", items1);

            List<String> items2 = List.of("IN-2026-016", "IN-2026-017");
            model.addAttribute("items2", items2);

            List<String> items3 = List.of("IN-2026-018", "IN-2026-019", "IN-2026-020");
            model.addAttribute("items3", items3);

            List<User> user = List.of(
                    User.builder().username("user1").managerName("사용자1").build(),
                    User.builder().username("user2").managerName("사용자2").build(),
                    User.builder().username("user3").managerName("사용자3").build(),
                    User.builder().username("user4").managerName("사용자4").build(),
                    User.builder().username("user5").managerName("사용자5").build());
            model.addAttribute("user", user);

            // Select Box - 담당자명
            selectBox.setManagerName("담당자");
            selectBox.setManagerName("김가람");
            selectBox.setManagerName("이나루");

            session.setAttribute("selectBox", selectBox);
            model.addAttribute("selectBox", selectBox);

            // 뷰 지정
            return "inspection";
        }
    }

    // 점검2 컨트롤러
    @Controller
    @RequiredArgsConstructor
    public class Inspection2Controller {
        private final InspectionService inspectionService;

        @GetMapping({ "/inspection2" })
        public String inspection2(
                @PathVariable(value = "page", required = false) String page,
                Model model,
                HttpSession session,
                User selectBox) {
            model.addAttribute("currentMenu", "inspection2");

            // 뷰 지정
            return "inspection2";
        }
    }

    // 유지보수 컨트롤러
    @Controller
    @RequiredArgsConstructor
    public class MaintenanceController {
        private final MaintenanceService maintenanceService;

        @GetMapping({ "/maintenance" }) // page추가
        public String maintenance(
                @PathVariable(value = "page", required = false) String page,
                Model model) {
            model.addAttribute("currentMenu", "maintenance");

            // 뷰 지정
            return "maintenance";
        }
    }

    // 유지보수2 컨트롤러
    @Controller
    @RequiredArgsConstructor
    public class Maintenance2Controller {
        private final MaintenanceService maintenanceService;

        @GetMapping({ "/maintenance2" }) // page추가
        public String maintenance2(
                @PathVariable(value = "page", required = false) String page,
                Model model) {
            model.addAttribute("currentMenu", "maintenance2");

            // 뷰 지정
            return "maintenance2";
        }
    }

    // 관리자검수 컨트롤러
    @Controller
    @RequiredArgsConstructor
    public class UserAuthController {
        private final UserService userService;

        @GetMapping({ "/user" }) // page추가
        public String user(
                @PathVariable(value = "page", required = false) String page,
                Model model) {
            model.addAttribute("currentMenu", "user");

            // 뷰 지정
            return "user";
        }
    }

}
