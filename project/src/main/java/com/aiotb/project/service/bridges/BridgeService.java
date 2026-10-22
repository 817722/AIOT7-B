package com.aiotb.project.service.bridges;

import com.aiotb.project.domain.bridges.Bridge;
import com.aiotb.project.dto.bridges.BridgeDto;
import com.aiotb.project.repository.bridges.BridgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BridgeService {

    private final BridgeRepository bridgeRepository;

    // 1. 교량 등록
    @Transactional
    public String createBridge(BridgeDto.Request request) {
        String uuid = UUID.randomUUID().toString();

        Bridge bridge = Bridge.builder()
                .id(uuid)
                .name(request.getName())
                .facilityType(request.getFacilityType())
                .province(request.getProvince())
                .city(request.getCity())
                .address(request.getAddress())
                .bridgeLength(request.getBridgeLength())
                .bridgeWidth(request.getBridgeWidth())
                .managementAgency(request.getManagementAgency())
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
                request.getManagementAgency()
        );
    }

    // 5. 교량 삭제
    @Transactional
    public void deleteBridge(String id) {
        Bridge bridge = bridgeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 교량이 존재하지 않습니다. id=" + id));
        bridgeRepository.delete(bridge);
    }
}