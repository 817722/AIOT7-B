package com.aiotb.project.service.maintenance;

import java.util.List;

import com.aiotb.project.dto.maintenance.MaintenanceDto;

public interface MaintenanceService {
    String create(MaintenanceDto.Request request, Long corpNo, Long userNo);

    List<MaintenanceDto.Response> byBridge(Long bridgeNo);

    List<MaintenanceDto.Response> all();

    MaintenanceDto.Response get(String id);

    void update(String id, MaintenanceDto.Request request, Long userNo);

    void delete(String id);
}
