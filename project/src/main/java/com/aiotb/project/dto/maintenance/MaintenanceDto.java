package com.aiotb.project.dto.maintenance;

import java.time.LocalDate;

import com.aiotb.project.domain.maintenance.Maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class MaintenanceDto {
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        @NotNull(message = "교량 번호는 필수입니다.")
        private Long bridgeNo;
        @NotBlank(message = "유지보수 기록명은 필수입니다.")
        private String title;
        @NotBlank(message = "유지보수 분류는 필수입니다.")
        private String maintenanceType;
        private String memo;
        @NotNull(message = "유지보수 날짜는 필수입니다.")
        private LocalDate maintenanceDate;
        @NotNull(message = "담당자 번호는 필수입니다.")
        private Long managerNo;
    }

    @Getter
    public static class Response {
        private final Long no;
        private final String id;
        private final Long bridgeNo;
        private final String title;
        private final String maintenanceType;
        private final String memo;
        private final LocalDate maintenanceDate;
        private final Long managerNo;

        public Response(Maintenance m) {
            no = m.getNo();
            id = m.getId();
            bridgeNo = m.getBridgeNo();
            title = m.getTitle();
            maintenanceType = m.getMaintenanceType();
            memo = m.getMemo();
            maintenanceDate = m.getMaintenanceDate();
            managerNo = m.getManagerNo();
        }
    }
}
