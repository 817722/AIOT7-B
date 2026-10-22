package com.aiotb.project.repository.corps;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiotb.project.domain.corps.Corp;
import com.aiotb.project.domain.users.User;

public interface CorpRepository extends JpaRepository<User, Long> {

    Optional<Corp> findByBisNo(String bisNo);

}