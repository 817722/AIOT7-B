package com.aiotb.project.repository.maintenance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiotb.project.domain.maintenance.Maintenance;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    Optional<Maintenance> findById(String id);

    List<Maintenance> findByBridgeNo(Long bridgeNo);

    List<Maintenance> findByMaintenanceDateBetween(LocalDate start, LocalDate end);

    List<Maintenance> findByMaintenanceType(String type);
}
