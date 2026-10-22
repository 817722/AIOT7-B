package com.aiotb.project.controller.defects;

import com.aiotb.project.dto.defects.DefectDto;
import com.aiotb.project.service.defects.DefectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/defects")
@RequiredArgsConstructor
public class DefectController {

    private final DefectService defectService;

    // 결함 등록 API
    @PostMapping
    public ResponseEntity<String> createDefect(@RequestBody @Valid DefectDto.Request request) {
        String defectId = defectService.createDefect(request);
        return ResponseEntity.ok(defectId);
    }

    // 특정 점검별 결함 목록 조회 API
    @GetMapping("/inspection/{inspectionNo}")
    public ResponseEntity<List<DefectDto.Response>> getDefectsByInspection(@PathVariable Long inspectionNo) {
        return ResponseEntity.ok(defectService.getDefectsByInspection(inspectionNo));
    }

    // 결함 상세 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<DefectDto.Response> getDefect(@PathVariable String id) {
        return ResponseEntity.ok(defectService.getDefectById(id));
    }

    // 결함 정보 수정 API
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateDefect(@PathVariable String id, @RequestBody @Valid DefectDto.Request request) {
        defectService.updateDefect(id, request);
        return ResponseEntity.ok().build();
    }

    // 결함 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDefect(@PathVariable String id) {
        defectService.deleteDefect(id);
        return ResponseEntity.ok().build();
    }
}