package com.aiotb.project.controller.bridges;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aiotb.project.dto.bridges.BridgeDto;
import com.aiotb.project.dto.bridges.BridgeDto.CatalogBridge;
import com.aiotb.project.service.bridges.BridgeCatalogService;
import com.aiotb.project.service.bridges.BridgeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bridges")
@RequiredArgsConstructor
public class BridgeController {

    private final BridgeService bridgeService;
    private final BridgeCatalogService bridgeCatalogService;

    // 교량 등록 API
    @PostMapping
    public ResponseEntity<String> createBridge(@RequestBody @Valid BridgeDto.CreateRequest request) {
        String bridgeId = bridgeService.createBridge(request);
        return ResponseEntity.ok(bridgeId);
    }

    // CSV 기준 교량명 검색. 예: /api/bridges/catalog?keyword=가람
    @GetMapping("/catalog")
    public ResponseEntity<List<CatalogBridge>> searchCatalog(
            @RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(bridgeCatalogService.search(keyword));
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