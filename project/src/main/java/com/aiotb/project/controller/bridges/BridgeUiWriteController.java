package com.aiotb.project.controller.bridges;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aiotb.project.service.bridges.BridgeUiWriteService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** 팀원 기존 /api/bridges REST와 충돌하지 않는, 기업/권한 검증을 수행하는 UI API. */
@RestController
@RequestMapping("/api/bridge-ui")
@RequiredArgsConstructor
public class BridgeUiWriteController {
    private final BridgeUiWriteService service;

    public record CreateRequest(@Min(1) long masterNo) {}
    public record UpdateRequest(@Min(1) long managerNo) {}

    @GetMapping("/catalog")
    public List<Map<String,Object>> catalog(@RequestParam("keyword") String keyword, Authentication auth) {
        return service.catalog(keyword, auth);
    }

    @GetMapping("/managers")
    public List<Map<String,Object>> managers(Authentication auth) {
        return service.managers(auth);
    }

    @PostMapping
    public ResponseEntity<Map<String,Long>> create(@Valid @RequestBody CreateRequest request, Authentication auth) {
        long no = service.create(request.masterNo(), auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("no", no));
    }

    @PutMapping("/{no}/manager")
    public ResponseEntity<Void> assign(@PathVariable("no") long no, @Valid @RequestBody UpdateRequest request,
                                       Authentication auth) {
        service.assignManager(no, request.managerNo(), auth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{no}")
    public ResponseEntity<Void> delete(@PathVariable("no") long no, Authentication auth) {
        service.delete(no, auth);
        return ResponseEntity.noContent().build();
    }
}
