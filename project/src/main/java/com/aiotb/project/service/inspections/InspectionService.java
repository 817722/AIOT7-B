package com.aiotb.project.service.inspections;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.domain.bridges.Bridge;
import com.aiotb.project.domain.inspections.Inspection;
import com.aiotb.project.dto.inspections.InspectionDto;
import com.aiotb.project.repository.bridges.BridgeRepository;
import com.aiotb.project.repository.inspections.InspectionRepository;

// 👈 드론 영상 업로드 코드 4개 추가(10/29)
import com.aiotb.project.domain.media.Media;
import com.aiotb.project.repository.media.MediaRepository;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionService {

        private final InspectionRepository inspectionRepository;
        private final BridgeRepository bridgeRepository;

        // 코드 추가(10/29) 
        private final MediaRepository mediaRepository;
        private final String uploadDir = System.getProperty("user.dir") + "/uploads/";
        //

        // 1. 점검 기록 등록 👈 코드 추가 (10/29)
        @Transactional
        public String createInspection(InspectionDto.Request request, MultipartFile file) {
                
                // DTO에 있는 숫자(bridgeNo)로 실제 Bridge 엔티티를 데이터베이스에서 찾아옵니다.
                Bridge bridge = bridgeRepository.findById(request.getBridgeNo())
                .orElseThrow(() -> new IllegalArgumentException("해당 교량이 존재하지 않습니다. 번호: " + request.getBridgeNo()));
                
                // 찾아온 bridge 객체를 Inspection 엔티티에 통째로 넣어줍니다.
                Inspection inspection = Inspection.builder()
                .id(UUID.randomUUID().toString().substring(0, 32)) // 고유 ID 자동 생성
                .bridge(bridge) // 👈 숫자가 아니라 아까 찾은 Bridge 객체를 넣습니다!
                .corpNo(request.getCorpNo())
                .title(request.getTitle())
                .inspectionType(request.getInspectionType())
                .status(request.getStatus())
                .inspectionDate(request.getInspectionDate())
                .inspectorNo(request.getInspectorNo())
                .inspectionMemo(request.getMemo()) // DTO의 memo를 엔티티의 inspectionMemo에 쏙
                .overallResult(request.getOverallResult())
                .reviewOpinion(request.getReviewOpinion())
                .createdBy(request.getCreatedBy())
                .build();
                
                // DB에 저장합니다.
                Inspection savedInspection = inspectionRepository.save(inspection);
                
                // 드론 파일이 있다면 여기서 미디어 테이블 저장 처리 (이전 단계에서 쓰시던 파일 저장 코드 넣기)
                if (file != null && !file.isEmpty()) {
                        // 파일 업로드 및 mediaRepository.save(...) 처리 영역
                        }
                        return savedInspection.getId();
                }
                
                // 1-1. 점검 기록 등록 + 드론 영상 파일(Media) 함께 처리 👈 코드 추가(10/29)
                @Transactional
                public String createInspectionWithMedia(InspectionDto.Request request, MultipartFile file) {
                        // 교량 존재 여부 확인
                        Bridge bridge = bridgeRepository.findById(request.getBridgeNo())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "해당 교량이 존재하지 않습니다. no=" + request.getBridgeNo()
                        ));
                        String uuid = UUID.randomUUID().toString();
                
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
                
                        // 드론 영상 파일이 존재할 경우 media 테이블 규격에 맞춰 저장
                        if (file != null && !file.isEmpty()) {
                                try {
                                     File dir = new File(uploadDir);
                                     if (!dir.exists()) {
                                        dir.mkdirs();
                                     }
                                     
                                     String originalFilename = file.getOriginalFilename();
                                     String savedFileName = UUID.randomUUID().toString() + "_" + originalFilename;
                                     File dest = new File(uploadDir + savedFileName);
                                     file.transferTo(dest);
                                     
                        // media 테이블 제약조건 충족 (corp_no 필수, p_type='INSPECTION', p_id=점검PK번호)
                        Media media = new Media(
                                request.getCorpNo(),                // corpNo (기업 번호 필수)
                                Media.ParentType.INSPECTION,        // parentType (부모 타입: INSPECTION)
                                savedInspection.getNo(),            // parentId (저장된 점검의 PK no)
                                Media.MediaType.VIDEO,              // type (미디어 유형: VIDEO)
                                false,                              // main (점검 파일이므로 false 고정)
                                uploadDir + savedFileName,          // path (파일 저장 경로)
                                originalFilename,                   // originName (원본 파일명)
                                file.getContentType(),              // mimeType (파일 MIME 타입)
                                file.getSize(),                     // fileSize (파일 크기)
                                request.getCreatedBy()              // uploadedBy (업로드 사용자 번호)
                        );
        
                        mediaRepository.save(media);
                        } catch (IOException e) {
                                throw new RuntimeException("드론 영상 파일 저장 실패: " + e.getMessage());
                            }
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