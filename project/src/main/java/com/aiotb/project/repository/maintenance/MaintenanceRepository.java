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

    // ── 기업별 접근 제한용 (로그인 사용자의 corp_no 기준) ──
    Optional<Maintenance> findByIdAndCorpNo(String id, Long corpNo);

    List<Maintenance> findByCorpNoOrderByMaintenanceDateDesc(Long corpNo);

    List<Maintenance> findByBridgeNoAndCorpNoOrderByMaintenanceDateDesc(Long bridgeNo, Long corpNo);
}
