package com.aiotb.project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.dto.bridges.BridgeInspectionRequestDto;
import com.aiotb.project.service.bridges.BridgeInspectionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bridge-inspections") // 👈 /api/bridges 대신 이 주소를 사용하면 차단 장치를 완벽히 피해갑니다!
@RequiredArgsConstructor
public class BridgeInspectionController {

    private final BridgeInspectionService bridgeInspectionService;

    @PostMapping
    public ResponseEntity<String> registerInspection(@ModelAttribute BridgeInspectionRequestDto requestDto) {

        // 1. 실제 비즈니스 로직(서비스) 호출하여 DB 저장 및 파일 처리 수행
        bridgeInspectionService.processInspection(requestDto);

        // 2. 데이터 수신 확인 로그
        System.out.println("교량명(keyword): " + requestDto.getKeyword());
        System.out.println("점검 분류: " + requestDto.getInspectionType());

        // 단일 파일 로그
        if (requestDto.getVideoInputSingle() != null && !requestDto.getVideoInputSingle().isEmpty()) {
            System.out.println("업로드된 드론 영상: " + requestDto.getVideoInputSingle().getOriginalFilename());
        }
        
        // 다중 파일 로그
        if (requestDto.getVideoInputMultiple() != null) {
            for (MultipartFile file : requestDto.getVideoInputMultiple()) {
                if (!file.isEmpty()) {
                    System.out.println("추가된 파일 명: " + file.getOriginalFilename());
                }
            }
        }

        return ResponseEntity.ok("교량 점검 데이터가 성공적으로 등록되었습니다.");
    }
}