package com.aiotb.project.controller.inspections;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.dto.inspections.InspectionDto;
import com.aiotb.project.service.inspections.InspectionService;
import tools.jackson.databind.ObjectMapper; // JSON 문자열 변환용

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createInspection(
            @RequestParam("request") String requestJson,
            @RequestPart(value = "file", required = false) MultipartFile file
        ) {
            try {
                // String으로 넘어온 JSON 데이터를 DTO 객체로 변환
                ObjectMapper objectMapper = new ObjectMapper();
                InspectionDto.Request request = objectMapper.readValue(requestJson, InspectionDto.Request.class);
                
                // 서비스로 데이터와 파일을 함께 전달
                String inspectionId = inspectionService.createInspection(request, file);
                return ResponseEntity.ok(inspectionId);
            
            } catch (Exception e) {
                throw new RuntimeException("점검 등록 및 업로드 실패: " + e.getMessage());
            }
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
    public ResponseEntity<Void> updateInspection(@PathVariable String id,
            @RequestBody @Valid InspectionDto.Request request) {
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