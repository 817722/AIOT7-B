package com.aiotb.project.service.defects;

import com.aiotb.project.domain.defects.DefectAction;
import com.aiotb.project.dto.defects.DefectActionDto;
import com.aiotb.project.repository.defects.DefectActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefectActionService {

    private final DefectActionRepository defectActionRepository;

    // 1. 조치 이력 등록
    @Transactional
    public String createAction(DefectActionDto.Request request) {
        String uuid = UUID.randomUUID().toString();

        DefectAction action = DefectAction.builder()
                .id(uuid)
                .defectNo(request.getDefectNo())
                .maintenanceNo(request.getMaintenanceNo())
                .status(request.getStatus())
                .actionDate(request.getActionDate())
                .managerNo(request.getManagerNo())
                .actionDescription(request.getActionDescription())
                .result(request.getResult())
                .completedAt(request.getCompletedAt())
                .createdBy(request.getCreatedBy())
                .build();

        return defectActionRepository.save(action).getId();
    }

    // 2. 특정 결함별 조치 이력 목록 조회
    public List<DefectActionDto.Response> getActionsByDefect(Long defectNo) {
        return defectActionRepository.findByDefectNo(defectNo).stream()
                .map(DefectActionDto.Response::new)
                .collect(Collectors.toList());
    }

    // 3. 조치 이력 상세 조회
    public DefectActionDto.Response getActionById(String id) {
        DefectAction action = defectActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 조치 이력이 존재하지 않습니다. id=" + id));
        return new DefectActionDto.Response(action);
    }

    // 4. 조치 이력 수정
    @Transactional
    public void updateAction(String id, DefectActionDto.Request request) {
        DefectAction action = defectActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 조치 이력이 존재하지 않습니다. id=" + id));

        action.updateAction(
                request.getStatus(),
                request.getActionDate(),
                request.getManagerNo(),
                request.getActionDescription(),
                request.getResult(),
                request.getCompletedAt()
        );
    }

    // 5. 조치 이력 삭제
    @Transactional
    public void deleteAction(String id) {
        DefectAction action = defectActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 조치 이력이 존재하지 않습니다. id=" + id));
        defectActionRepository.delete(action);
    }
}