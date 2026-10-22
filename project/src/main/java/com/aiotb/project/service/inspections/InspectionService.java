package com.aiotb.project.service.inspections;

import com.aiotb.project.domain.bridges.Bridge;
import com.aiotb.project.domain.inspections.Inspection;
import com.aiotb.project.dto.inspections.InspectionDto;
import com.aiotb.project.repository.bridges.BridgeRepository;
import com.aiotb.project.repository.inspections.InspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final BridgeRepository bridgeRepository;

    // 1. 점검 기록 등록
    @Transactional
    public String createInspection(InspectionDto.Request request) {
        Bridge bridge = bridgeRepository.findById(request.getBridgeNo())
                .orElseThrow(() -> new IllegalArgumentException("해당 교량이 존재하지 않습니다. no=" + request.getBridgeNo()));

        String uuid = UUID.randomUUID().toString();

        Inspection inspection = Inspection.builder()
                .id(uuid)
                .bridge(bridge)
                .corpNo(request.getCorpNo())
                .inspectionType(request.getInspectionType())
                .status(request.getStatus())
                .inspectionDate(request.getInspectionDate())
                .inspectorNo(request.getInspectorNo())
                .overallResult(request.getOverallResult())
                .reviewOpinion(request.getReviewOpinion())
                .createdBy(request.getCreatedBy())
                .build();

        return inspectionRepository.save(inspection).getId();
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
                request.getInspectionType(),
                request.getStatus(),
                request.getInspectionDate(),
                request.getOverallResult(),
                request.getReviewOpinion()
        );
    }

    // 5. 점검 기록 삭제
    @Transactional
    public void deleteInspection(String id) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 점검 기록이 존재하지 않습니다. id=" + id));
        inspectionRepository.delete(inspection);
    }
}