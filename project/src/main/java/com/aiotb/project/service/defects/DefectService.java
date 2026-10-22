package com.aiotb.project.service.defects;

import com.aiotb.project.domain.defects.Defect;
import com.aiotb.project.domain.inspections.Inspection;
import com.aiotb.project.dto.defects.DefectDto;
import com.aiotb.project.repository.defects.DefectRepository;
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
public class DefectService {

    private final DefectRepository defectRepository;
    private final InspectionRepository inspectionRepository;

    // 1. 결함 등록
    @Transactional
    public String createDefect(DefectDto.Request request) {
        Inspection inspection = inspectionRepository.findById(request.getInspectionNo())
                .orElseThrow(() -> new IllegalArgumentException("해당 점검 기록이 존재하지 않습니다. no=" + request.getInspectionNo()));

        String uuid = UUID.randomUUID().toString();

        Defect defect = Defect.builder()
                .id(uuid)
                .inspection(inspection)
                .defectType(request.getDefectType())
                .location(request.getLocation())
                .severity(request.getSeverity())
                .description(request.getDescription())
                .createdBy(request.getCreatedBy())
                .build();

        return defectRepository.save(defect).getId();
    }

    // 2. 특정 점검의 결함 목록 조회
    public List<DefectDto.Response> getDefectsByInspection(Long inspectionNo) {
        return defectRepository.findByInspectionNo(inspectionNo).stream()
                .map(DefectDto.Response::new)
                .collect(Collectors.toList());
    }

    // 3. 결함 상세 조회
    public DefectDto.Response getDefectById(String id) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 결함 정보가 존재하지 않습니다. id=" + id));
        return new DefectDto.Response(defect);
    }

    // 4. 결함 정보 수정
    @Transactional
    public void updateDefect(String id, DefectDto.Request request) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 결함 정보가 존재하지 않습니다. id=" + id));

        defect.updateDefect(
                request.getDefectType(),
                request.getLocation(),
                request.getSeverity(),
                request.getDescription()
        );
    }

    // 5. 결함 삭제
    @Transactional
    public void deleteDefect(String id) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 결함 정보가 존재하지 않습니다. id=" + id));
        defectRepository.delete(defect);
    }
}