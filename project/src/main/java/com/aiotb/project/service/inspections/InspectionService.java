package com.aiotb.project.service.inspections;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.domain.bridges.Bridge;
import com.aiotb.project.domain.inspections.Inspection;
import com.aiotb.project.domain.media.Media;
import com.aiotb.project.dto.inspections.InspectionDto;
import com.aiotb.project.repository.bridges.BridgeRepository;
import com.aiotb.project.repository.inspections.InspectionRepository;
import com.aiotb.project.service.media.MediaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionService {

        private final InspectionRepository inspectionRepository;
        private final BridgeRepository bridgeRepository;
        // 미디어 전용 서비스를 주입받습니다.
        private final MediaService mediaService;

        // 1. 점검 기록 등록
        @Transactional
        public String createInspection(InspectionDto.Request request, MultipartFile file) {
                
                // DTO에 있는 숫자(bridgeNo)로 실제 Bridge 엔티티를 데이터베이스에서 찾아옵니다.
                Bridge bridge = bridgeRepository.findById(request.getBridgeNo())
                        .orElseThrow(() -> new IllegalArgumentException("해당 교량이 존재하지 않습니다. 번호: " + request.getBridgeNo()));
                
                // 찾아온 bridge 객체를 Inspection 엔티티에 통째로 넣어줍니다.
                Inspection inspection = Inspection.builder()
                        .id(UUID.randomUUID().toString().substring(0, 32)) // 고유 ID 자동 생성
                        .bridge(bridge) 
                        .corpNo(request.getCorpNo())
                        .title(request.getTitle())
                        .inspectionType(request.getInspectionType())
                        .status(request.getStatus())
                        .inspectionDate(request.getInspectionDate())
                        .inspectorNo(request.getInspectorNo())
                        .inspectionMemo(request.getMemo())
                        .overallResult(request.getOverallResult())
                        .reviewOpinion(request.getReviewOpinion())
                        .createdBy(request.getCreatedBy())
                        .build();
                
                // DB에 저장합니다.
                Inspection savedInspection = inspectionRepository.save(inspection);
                
                // 복잡한 파일 저장 코드는 지우고, MediaService에 깔끔하게 위임합니다!
                        if (file != null && !file.isEmpty()) {
                                mediaService.saveFile(
                                        file, 
                                        request.getCorpNo(), 
                                        Media.ParentType.INSPECTION, 
                                        Long.valueOf(savedInspection.getId()), // parentId가 Long 타입인 경우 형식에 맞춤
                                        request.getCreatedBy()
                                );
                        }
                        
                        return savedInspection.getId();
                }
                
                // 1-1. 점검 기록 등록 + 드론 영상 파일(Media) 함께 처리
                @Transactional
                public String createInspectionWithMedia(InspectionDto.Request request, MultipartFile file) {
                        // 교량 존재 여부 확인
                        Bridge bridge = bridgeRepository.findById(request.getBridgeNo())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "해당 교량이 존재하지 않습니다. no=" + request.getBridgeNo()
                        ));
                        String uuid = UUID.randomUUID().toString().substring(0, 32);
                
                        // 점검 본문 저장
                        Inspection inspection = Inspection.builder()
                                .id(uuid)
                                .bridge(bridge)
                                .corpNo(request.getCorpNo())
                                .title(request.getTitle())
                                .inspectionType(request.getInspectionType())
                                .status(request.getStatus())
                                .inspectionDate(request.getInspectionDate())
                                .inspectorNo(request.getInspectorNo())
                                .inspectionMemo(request.getMemo())
                                .overallResult(request.getOverallResult())
                                .reviewOpinion(request.getReviewOpinion())
                                .createdBy(request.getCreatedBy())
                                .build();
                        
                        Inspection savedInspection = inspectionRepository.save(inspection);

                        // 복잡한 파일 저장 코드는 지우고, MediaService에 깔끔하게 위임합니다!
                        if (file != null && !file.isEmpty()) {
                                mediaService.saveFile(
                                        file, 
                                        request.getCorpNo(), 
                                        Media.ParentType.INSPECTION, 
                                        Long.valueOf(savedInspection.getId()), // String ID 전달
                                        request.getCreatedBy()
                                );
                        }
                        
                        return savedInspection.getId();
                }

        // 2. 특정 교량의 점검 목록 조회
        public List<InspectionDto.Response> getInspectionsByBridge(Long bridgeNo) {
                return inspectionRepository.findByBridgeNo(bridgeNo).stream()
                                .map(InspectionDto.Response::new)
                                .collect(Collectors.toList());
        }

        // 3. 점검 상세 조회
        public InspectionDto.Response getInspectionById(String id) {
                Inspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("해당 점검 기록이 존재하지 않습니다. id=" + id));
                return new InspectionDto.Response(inspection);
        }

        // 4. 점검 기록 수정
        @Transactional
        public void updateInspection(String id, InspectionDto.Request request) {
                Inspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("해당 점검 기록이 존재하지 않습니다. id=" + id));

                inspection.updateInspection(
                                request.getTitle(),
                                request.getInspectionType(),
                                request.getStatus(),
                                request.getInspectionDate(),
                                request.getMemo(),
                                request.getOverallResult(),
                                request.getReviewOpinion());
        }

        // 5. 점검 기록 삭제
        @Transactional
        public void deleteInspection(String id) {
                Inspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("해당 점검 기록이 존재하지 않습니다. id=" + id));
                inspectionRepository.delete(inspection);
        }
}