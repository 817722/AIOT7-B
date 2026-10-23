package com.aiotb.project.repository.corps;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiotb.project.domain.corps.Corp;

public interface CorpRepository extends JpaRepository<Corp, Long> {

    Optional<Corp> findByBisNo(String bisNo);

}