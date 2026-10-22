package com.aiotb.project.controller.defects;

import com.aiotb.project.dto.defects.DefectActionDto;
import com.aiotb.project.service.defects.DefectActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/defect-actions")
@RequiredArgsConstructor
public class DefectActionController {

    private final DefectActionService defectActionService;

    // 조치 이력 등록 API
    @PostMapping
    public ResponseEntity<String> createAction(@RequestBody @Valid DefectActionDto.Request request) {
        String actionId = defectActionService.createAction(request);
        return ResponseEntity.ok(actionId);
    }

    // 특정 결함별 조치 이력 목록 조회 API
    @GetMapping("/defect/{defectNo}")
    public ResponseEntity<List<DefectActionDto.Response>> getActionsByDefect(@PathVariable Long defectNo) {
        return ResponseEntity.ok(defectActionService.getActionsByDefect(defectNo));
    }

    // 조치 이력 상세 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<DefectActionDto.Response> getAction(@PathVariable String id) {
        return ResponseEntity.ok(defectActionService.getActionById(id));
    }

    // 조치 이력 수정 API
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateAction(@PathVariable String id, @RequestBody @Valid DefectActionDto.Request request) {
        defectActionService.updateAction(id, request);
        return ResponseEntity.ok().build();
    }

    // 조치 이력 삭제 API
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAction(@PathVariable String id) {
        defectActionService.deleteAction(id);
        return ResponseEntity.ok().build();
    }
}