package com.aiotb.project.service.maintenance;

import java.util.List;
import com.aiotb.project.dto.maintenance.MaintenanceDto;

import org.springframework.web.multipart.MultipartFile; // 👈 코드 추가(10/29)

public interface MaintenanceService {
    // String create(MaintenanceDto.Request request, Long corpNo, Long userNo); 👈 코드 백업용(10/29)

    String create(MaintenanceDto.Request request, MultipartFile file, Long corpNo, Long userNo); // 👈 코드 수정(10/29)

    List<MaintenanceDto.Response> byBridge(Long bridgeNo);

    List<MaintenanceDto.Response> all();

    MaintenanceDto.Response get(String id);

    void update(String id, MaintenanceDto.Request request, Long userNo);

    void delete(String id);
}
