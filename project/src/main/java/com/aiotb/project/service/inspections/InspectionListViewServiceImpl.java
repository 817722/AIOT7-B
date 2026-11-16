package com.aiotb.project.service.inspections;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.dto.inspections.InspectionDto;
import com.aiotb.project.repository.inspections.InspectionRepository;
import com.aiotb.project.service.users.CurrentUserService.Actor;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionListViewServiceImpl {

    private final InspectionRepository inspectionRepository;

    // 특정 교량의 전체 점검 목록 조회
    public List<InspectionDto.Response> getInspectionListByBridge(
            Long bridgeNo, Actor actor) {

        return inspectionRepository
                .findByBridgeNoAndCorpNoOrderByInspectionDateDesc(
                        bridgeNo, actor.corpNo())
                .stream()
                .map(InspectionDto.Response::new)
                .collect(Collectors.toList());
    }

    // 기업 전체 소속의 점검 목록 조회
    public List<InspectionDto.Response> getAllInspectionsForCorp(Actor actor) {

        return inspectionRepository
                .findByCorpNoOrderByInspectionDateDesc(actor.corpNo())
                .stream()
                .map(InspectionDto.Response::new)
                .toList();
    }
}