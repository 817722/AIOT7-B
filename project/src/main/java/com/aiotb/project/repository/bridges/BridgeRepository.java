package com.aiotb.project.repository.bridges;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiotb.project.domain.bridges.Bridge;

public interface BridgeRepository extends JpaRepository<Bridge, Long> {

    // 고유 UUID 식별자로 조회
    Optional<Bridge> findById(String id);

    // 지역(시도, 시군구) 필터 검색
    List<Bridge> findByProvinceAndCity(String province, String city);

    // 시설물 종류(제1종, 제2종 등) 필터 검색
    List<Bridge> findByFacilityType(String facilityType);
}