package com.aiotb.project.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security 설정
 */
@Slf4j
@Configuration // 스프링 빈 설정 클래스로 지정
@EnableWebSecurity // 스프리 시큐리티 설정 클래스로 지정
@RequiredArgsConstructor // 필수 매개변수 생성자 자동 생성
public class SecurityConfig {

    // private final CustomUserDetailsService customUserDetailsService;
    private final DataSource dataSource;

    // ⚡ 스프링 시큐리티 설정 방법
    // - SecurityFilterChain 을 빈 등록해서 인가설정, 폼로그인 설정 등을 한다.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 인가 설정
        http.authorizeHttpRequests(auth -> auth
                // 공개 페이지, 정적 자원
                // TODO: 우리 프로젝트 목적에 맞게 경로 수정 필요
                .requestMatchers("/", "/css/**", "/js/**", "/img/**", "/images/**").permitAll()
                .requestMatchers("/users/join", "/users/login").permitAll()
                .requestMatchers("/products", "/products/**").permitAll()
                // 로그인 필요 영역
                .requestMatchers("/cart/**", "/orders/**", "/mypage/**").authenticated()
                .anyRequest().permitAll());

<<<<<<< HEAD
    // 폼 로그인
    http.formLogin(login -> login
          .loginPage("/login")
          .loginProcessingUrl("/users/login")
          .usernameParameter("username")
          .passwordParameter("password")
          .defaultSuccessUrl("/", true)
          .failureUrl("/login?error=true")
          .permitAll()
    );
=======
        // 폼 로그인
        http.formLogin(login -> login
                .loginPage("/users/login")
                .loginProcessingUrl("/users/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureUrl("/users/login?error=true")
                .permitAll());
>>>>>>> 31a9f5420464c5c1e02de15abb4774c5d908c549

        // TODO:사용자 정의 인증

        // TODO:자동 로그인

        // 로그아웃
        http.logout(logout -> logout
                .logoutUrl("/users/logout")
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"));

        return http.build();
    }

    // 비밀번호 암호화 객체 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PersistentTokenRepository tokenRepository() {
        // JdbcTokenRepositoryImpl : 토큰 저장 데이터 베이스를 등록하는 객체
        JdbcTokenRepositoryImpl repositoryImpl = new JdbcTokenRepositoryImpl();
        // ✅ 토큰 저장소를 사용하는 데이터 소스 지정
        // - SpringSecurity 가 자동 로그인 프로세스를 처리하기 위한 DB를 지정합니다.
        repositoryImpl.setDataSource(dataSource);

        // persistent_logins 테이블 생성
        try {
            repositoryImpl.getJdbcTemplate().execute(JdbcTokenRepositoryImpl.CREATE_TABLE_SQL);
        } catch (BadSqlGrammarException e) {
            log.error("persistent_logins 테이블이 이미 존재합니다.");
        } catch (Exception e) {
            log.error("자동 로그인 테이블 생성 중, 예외 발생");
        }
        return repositoryImpl;
    }

}
