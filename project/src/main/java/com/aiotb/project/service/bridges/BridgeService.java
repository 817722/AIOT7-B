package com.aiotb.project.service.bridges;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.domain.bridges.Bridge;
import com.aiotb.project.dto.bridges.BridgeDto;
import com.aiotb.project.repository.bridges.BridgeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BridgeService {

    private final BridgeRepository bridgeRepository;
    private final BridgeCatalogService bridgeCatalogService;

    // 1. 교량 등록
    @Transactional
    public String createBridge(BridgeDto.CreateRequest request) {
        // 클라이언트가 교량 정보를 직접 작성하지 못하도록 CSV 식별자로만 조회합니다.
        BridgeDto.CatalogBridge selected = bridgeCatalogService.getById(request.getCatalogId());
        String uuid = UUID.randomUUID().toString();

        Bridge bridge = Bridge.builder()
                .id(uuid)
                .sourceDataDate(selected.sourceDataDate())
                .name(selected.name())
                .facilityType(selected.facilityType())
                .roadType(selected.roadType())
                .roadName(selected.roadName())
                .province(selected.province())
                .city(selected.city())
                .address(selected.address())
                .bridgeLength(selected.bridgeLength())
                .bridgeWidth(selected.bridgeWidth())
                .managementAgency(selected.managementAgency())
                .createdBy(request.getCreatedBy() != null ? request.getCreatedBy() : 1L) // 기본값 설정
                .build();

        return bridgeRepository.save(bridge).getId();
    }

    // 2. 교량 목록 조회
    public List<BridgeDto.Response> getAllBridges() {
        return bridgeRepository.findAll().stream()
                .map(BridgeDto.Response::new)
                .collect(Collectors.toList());
    }

    // 3. 교량 상세 조회
    public BridgeDto.Response getBridgeById(String id) {
        Bridge bridge = bridgeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 교량이 존재하지 않습니다. id=" + id));
        return new BridgeDto.Response(bridge);
    }

    // 4. 교량 수정
    @Transactional
    public void updateBridge(String id, BridgeDto.Request request) {
        Bridge bridge = bridgeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 교량이 존재하지 않습니다. id=" + id));

        bridge.updateBridge(
                request.getName(),
                request.getFacilityType(),
                request.getAddress(),
                request.getProvince(),
                request.getCity(),
                request.getBridgeLength(),
                request.getBridgeWidth(),
                request.getManagementAgency());
    }

    // 5. 교량 삭제
    @Transactional
    public void deleteBridge(String id) {
        Bridge bridge = bridgeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 교량이 존재하지 않습니다. id=" + id));
        bridgeRepository.delete(bridge);
    }
}