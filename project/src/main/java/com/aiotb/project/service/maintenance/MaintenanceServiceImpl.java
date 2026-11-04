package com.aiotb.project.service.maintenance;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID; // 👈 코드 추가(10/29)

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile; // 👈 코드 추가(10/29)

import com.aiotb.project.domain.maintenance.Maintenance;
import com.aiotb.project.dto.maintenance.MaintenanceDto;
import com.aiotb.project.repository.maintenance.MaintenanceRepository;
import com.aiotb.project.domain.bridges.Bridge; // 👈 교량 엔티티 임포트 추가(10/29)
import com.aiotb.project.repository.bridges.BridgeRepository; // 👈 교량 레포지토리 임포트 추가(10/29)


import lombok.RequiredArgsConstructor;

// corpNo/userNo는 인증 principal에서 받아야됨!
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceServiceImpl implements MaintenanceService {
    private final MaintenanceRepository repository;
    private final BridgeRepository bridgeRepository; // 👈 교량 조회용 레포지토리 추가(10/29)

    // @Override 👈 이전 코드 백업용 (10/29)
    // @Transactional
    // public String create(MaintenanceDto.Request r, Long corpNo, Long userNo) {
    //     validateType(r.getMaintenanceType());
    //     Maintenance m = new Maintenance(r.getBridgeNo(), corpNo, r.getTitle(), r.getMaintenanceType(),
    //             r.getMemo(), r.getMaintenanceDate(), r.getManagerNo(), userNo);
    //     return repository.save(m).getId();
    // }
    //
    
    @Override
    @Transactional
    public String create(MaintenanceDto.Request r, MultipartFile file, Long corpNo, Long userNo) {
        validateType(r.getMaintenanceType());
        
        // 1. 엔티티 구조에 맞춰서 bridgeNo 숫자를 그대로 넣어 Maintenance 객체를 생성합니다.
        Maintenance m = new Maintenance(
                r.getBridgeNo(),       // bridgeNo (Long)
                corpNo,                // corpNo (Long)
                r.getTitle(),          // title (String)
                r.getMaintenanceType(),// maintenanceType (String)
                r.getMemo(),           // memo (String)
                r.getMaintenanceDate(),// maintenanceDate (LocalDate)
                r.getManagerNo(),      // managerNo (Long)
                userNo                 // createdBy (Long)
        );

        // 2. 데이터베이스에 저장합니다.
        String maintenanceId = repository.save(m).getId();

        // 3. 파일(드론 영상 등)이 존재할 경우 처리하는 공간
        if (file != null && !file.isEmpty()) {
            // 파일 업로드 및 mediaRepository 저장 로직
        }

        return maintenanceId;
    }

    @Override
    public List<MaintenanceDto.Response> byBridge(Long bridgeNo) {
        return repository.findByBridgeNo(bridgeNo).stream().map(MaintenanceDto.Response::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<MaintenanceDto.Response> all() {
        return repository.findAll().stream().map(MaintenanceDto.Response::new).collect(Collectors.toList());
    }

    @Override
    public MaintenanceDto.Response get(String id) {
        return new MaintenanceDto.Response(
                repository.findById(id).orElseThrow(() -> new IllegalArgumentException("유지보수 기록을 찾을 수 없습니다.")));
    }

    @Override
    @Transactional
    public void update(String id, MaintenanceDto.Request r, Long userNo) {
        validateType(r.getMaintenanceType());
        Maintenance m = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("유지보수 기록을 찾을 수 없습니다."));
        m.update(r.getTitle(), r.getMaintenanceType(), r.getMemo(), r.getMaintenanceDate(), r.getManagerNo(), userNo);
    }

    @Override
    @Transactional
    public void delete(String id) {
        repository
                .delete(repository.findById(id).orElseThrow(() -> new IllegalArgumentException("유지보수 기록을 찾을 수 없습니다.")));
    }

    private void validateType(String type) {
        if (!List.of("균열 보수기록", "단면 복구기록", "구조 보강기록", "하자보수 관리", "기타").contains(type))
            throw new IllegalArgumentException("지원하지 않는 유지보수 분류입니다.");
    }
}
