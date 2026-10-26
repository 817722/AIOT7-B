package com.aiotb.project.controller.maintenance;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.dto.maintenance.MaintenanceDto;
import com.aiotb.project.repository.users.UserRepository;
import com.aiotb.project.service.maintenance.MaintenanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {
    private final MaintenanceService service;
    private final UserRepository users;

    private User current(UserDetails principal) {
        if (principal == null)
            throw new IllegalStateException("로그인이 필요합니다.");
        return users.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인 사용자 정보를 찾을 수 없습니다."));
    }

    @PostMapping
    public String create(@Valid @RequestBody MaintenanceDto.Request request,
            @AuthenticationPrincipal UserDetails principal) {
        User user = current(principal);
        return service.create(request, user.getCorp().getNo(), user.getNo());
    }

    @GetMapping
    public List<MaintenanceDto.Response> all() {
        return service.all();
    }

    @GetMapping("/bridge/{bridgeNo}")
    public List<MaintenanceDto.Response> byBridge(@PathVariable Long bridgeNo) {
        return service.byBridge(bridgeNo);
    }

    @GetMapping("/{id}")
    public MaintenanceDto.Response get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable String id, @Valid @RequestBody MaintenanceDto.Request request,
            @AuthenticationPrincipal UserDetails principal) {
        service.update(id, request, current(principal).getNo());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
