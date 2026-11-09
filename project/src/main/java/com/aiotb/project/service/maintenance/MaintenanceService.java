package com.aiotb.project.service.maintenance;

import java.util.List;
import com.aiotb.project.dto.maintenance.MaintenanceDto;
import com.aiotb.project.service.users.CurrentUserService.Actor;

/** 유지보수 기록 REST(/api/maintenance) 서비스. 모든 메서드는 로그인 사용자(Actor)의 기업 범위로 제한된다. */
public interface MaintenanceService {

    String create(MaintenanceDto.Request request, Actor actor);

    List<MaintenanceDto.Response> byBridge(Long bridgeNo, Actor actor);

    List<MaintenanceDto.Response> all(Actor actor);

    MaintenanceDto.Response get(String id, Actor actor);

    void update(String id, MaintenanceDto.Request request, Actor actor);

    void delete(String id, Actor actor);
}
