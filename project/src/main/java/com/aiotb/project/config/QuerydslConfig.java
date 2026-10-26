package com.aiotb.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// QueryDSL 설정 
// - JPAQueryFactory 빈 등록
@Configuration            			// 스프링 빈 설정 클래스로 지정
@EnableJpaAuditing        			// ⭐ JPA Auditing 기능 활성화 (등록일, 수정일 자동 기록)
public class QuerydslConfig {

  	@PersistenceContext     		// JPA EntityManager 의존성 주입
  	private EntityManager entityManager;

  	@Bean                   		// 빈 등록
  	public JPAQueryFactory JPAQueryFactory() {
  	  	return new JPAQueryFactory(entityManager);
  	}
  
}
