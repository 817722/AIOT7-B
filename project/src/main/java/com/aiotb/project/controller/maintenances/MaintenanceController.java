package com.aiotb.project.controller.maintenances;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiotb.project.dto.maintenance.MaintenanceDto;
import com.aiotb.project.service.maintenance.MaintenanceService;
import com.aiotb.project.service.users.CurrentUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 유지보수 기록 REST API.
 * - 접근: 로그인 + 승인된 사용자 — SecurityConfig / @PreAuthorize
 * - 삭제: ADMIN 전용
 * - 데이터 범위: 로그인 사용자의 소속 기업 — MaintenanceService
 */
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSPECTOR', 'SUPER_ADMIN')")
public class MaintenanceController {
    private final MaintenanceService service;
    private final CurrentUserService currentUserService;

    // 유지보수 본문만 JSON(@RequestBody)으로 등록
    @PostMapping
    public String create(@Valid @RequestBody MaintenanceDto.Request request) {
        return service.create(request, currentUserService.current());
    }

    @GetMapping
    public List<MaintenanceDto.Response> all() {
        return service.all(currentUserService.current());
    }

    @GetMapping("/bridge/{bridgeNo}")
    public List<MaintenanceDto.Response> byBridge(@PathVariable("bridgeNo") Long bridgeNo) {
        return service.byBridge(bridgeNo, currentUserService.current());
    }

    @GetMapping("/{id}")
    public MaintenanceDto.Response get(@PathVariable("id") String id) {
        return service.get(id, currentUserService.current());
    }

    @PutMapping("/{id}")
    public void update(@PathVariable("id") String id, @Valid @RequestBody MaintenanceDto.Request request) {
        service.update(id, request, currentUserService.current());
    }

    // 유지보수 기록 삭제 (기업 관리자 전용)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        service.delete(id, currentUserService.current());
    }
}
