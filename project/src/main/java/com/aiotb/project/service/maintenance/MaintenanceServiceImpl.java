package com.aiotb.project.service.maintenance;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.domain.maintenance.Maintenance;
import com.aiotb.project.dto.maintenance.MaintenanceDto;
import com.aiotb.project.repository.maintenance.MaintenanceRepository;

import lombok.RequiredArgsConstructor;

// corpNo/userNo는 인증 principal에서 받아야됨!
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceServiceImpl implements MaintenanceService {
    private final MaintenanceRepository repository;

    @Override
    @Transactional
    public String create(MaintenanceDto.Request r, Long corpNo, Long userNo) {
        validateType(r.getMaintenanceType());
        Maintenance m = new Maintenance(r.getBridgeNo(), corpNo, r.getTitle(), r.getMaintenanceType(),
                r.getMemo(), r.getMaintenanceDate(), r.getManagerNo(), userNo);
        return repository.save(m).getId();
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
