package com.aiotb.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aiotb.project.domain.BridgeInspectionEntity; // 밑에서 만들 엔티티

@Repository 
public interface BridgeInspectionRepository extends JpaRepository<BridgeInspectionEntity, Long> {
    // 이 인터페이스를 만들어두기만 하면, 스프링이 알아서 DB에 저장/조회하는 기능을 다 만들어줍니다!
}
