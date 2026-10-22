package com.aiotb.project.controller.bridges;

import com.aiotb.project.dto.bridges.BridgeDto;
import com.aiotb.project.service.bridges.BridgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bridges")
@RequiredArgsConstructor
public class BridgeController {

    private final BridgeService bridgeService;

    // 교량 등록 API
    @PostMapping
    public ResponseEntity<String> createBridge(@RequestBody @Valid BridgeDto.Request request) {
        String bridgeId = bridgeService.createBridge(request);
        return ResponseEntity.ok(bridgeId);
    }

    // 교량 전체 목록 조회 API
    @GetMapping
    public ResponseEntity<List<BridgeDto.Response>> getAllBridges() {
        return ResponseEntity.ok(bridgeService.getAllBridges());
    }

    // 교량 상세 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<BridgeDto.Response> getBridge(@PathVariable String id) {
        return ResponseEntity.ok(bridgeService.getBridgeById(id));
    }

    // 교량 정보 수정 API
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateBridge(@PathVariable String id, @RequestBody @Valid BridgeDto.Request request) {
        bridgeService.updateBridge(id, request);
        return ResponseEntity.ok().build();
    }

    // 교량 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBridge(@PathVariable String id) {
        bridgeService.deleteBridge(id);
        return ResponseEntity.ok().build();
    }
}