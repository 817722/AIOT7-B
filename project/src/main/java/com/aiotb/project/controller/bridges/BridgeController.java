package com.aiotb.project.controller.bridges;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.dto.bridges.BridgeDto;
import com.aiotb.project.dto.bridges.BridgeDto.CatalogBridge;
import com.aiotb.project.dto.inspections.InspectionDto; // 👈 통합된 DTO 임포트
import com.aiotb.project.dto.tests.BridgeInspectionRequestDto;
import com.aiotb.project.service.bridges.BridgeCatalogService;
import com.aiotb.project.service.bridges.BridgeService;
import com.aiotb.project.service.inspections.InspectionService; // 👈 통합된 서비스 임포트
import com.aiotb.project.service.users.CurrentUserService.Actor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bridges")
@RequiredArgsConstructor
public class BridgeController {

    private final BridgeService bridgeService;
    private final BridgeCatalogService bridgeCatalogService;
    private final InspectionService inspectionService; // 👈 통합된 서비스 주입

    // 기존 교량 등록 API (주소: POST /api/bridges)
    @PostMapping
    public ResponseEntity<String> createBridge(@RequestBody @Valid BridgeDto.CreateRequest request) {
        String bridgeId = bridgeService.createBridge(request);
        return ResponseEntity.ok(bridgeId);
    }

    // 교량 점검 데이터 및 파일 등록 API (주소: POST /api/bridges/inspection)
    @PostMapping("/inspection")
    public ResponseEntity<String> registerInspection(
            @ModelAttribute @Valid InspectionDto.Request requestDto,
            @AuthenticationPrincipal Actor actor) {

        // 통합된 InspectionService를 호출하여 DB 저장 및 파일 처리 수행
        String inspectionId = inspectionService.createInspection(requestDto, actor);

        // 디버깅 및 확인용 로그 출력
        System.out.println("교량 번호(bridgeNo): " + requestDto.getBridgeNo());
        System.out.println("점검 기록명(title): " + requestDto.getTitle());
        System.out.println("점검 분류: " + requestDto.getInspectionType());
        System.out.println("진행 상태: " + requestDto.getStatus());

        // 드론 영상 파일 처리 (단일)
        if (requestDto.getVideoInputSingle() != null && !requestDto.getVideoInputSingle().isEmpty()) {
            String droneVideoName = requestDto.getVideoInputSingle().getOriginalFilename();
            System.out.println("업로드된 드론 영상: " + droneVideoName);
        }
        
        // 파일 추가 처리 (다중)
        if (requestDto.getVideoInputMultiple() != null) {
            for (MultipartFile file : requestDto.getVideoInputMultiple()) {
                if (!file.isEmpty()) {
                    System.out.println("추가된 파일 명: " + file.getOriginalFilename());
                }
            }
        }

        // 정상 등록 완료 응답 (생성된 고유 ID 반환)
        return ResponseEntity.ok("교량 점검 데이터가 성공적으로 등록되었습니다. (ID: " + inspectionId + ")");
    }

    // CSV 기준 교량명 검색
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