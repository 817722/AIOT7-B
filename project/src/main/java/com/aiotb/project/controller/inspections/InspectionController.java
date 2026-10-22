package com.aiotb.project.controller.inspections;

import com.aiotb.project.dto.inspections.InspectionDto;
import com.aiotb.project.service.inspections.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    // 점검 기록 등록 API
    @PostMapping
    public ResponseEntity<String> createInspection(@RequestBody @Valid InspectionDto.Request request) {
        String inspectionId = inspectionService.createInspection(request);
        return ResponseEntity.ok(inspectionId);
    }

    // 특정 교량별 점검 목록 조회 API
    @GetMapping("/bridge/{bridgeNo}")
    public ResponseEntity<List<InspectionDto.Response>> getInspectionsByBridge(@PathVariable Long bridgeNo) {
        return ResponseEntity.ok(inspectionService.getInspectionsByBridge(bridgeNo));
    }

    // 점검 상세 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<InspectionDto.Response> getInspection(@PathVariable String id) {
        return ResponseEntity.ok(inspectionService.getInspectionById(id));
    }

    // 점검 기록 수정 API
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateInspection(@PathVariable String id, @RequestBody @Valid InspectionDto.Request request) {
        inspectionService.updateInspection(id, request);
        return ResponseEntity.ok().build();
    }

    // 점검 기록 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInspection(@PathVariable String id) {
        inspectionService.deleteInspection(id);
        return ResponseEntity.ok().build();
    }
}