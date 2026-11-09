package com.aiotb.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.aiotb.project.service.users.CustomUserDetailsService;
import com.aiotb.project.service.users.LoginStatusChecker;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize 사용
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;

    @Bean  // 👈 위에 있는 Bean 주석으로 놓고 새로 추가(11/04)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http.authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/", "/login", "/join", "/users/join",
                "/users/login", "/api/business/verify", "/error",
                "/css/**", "/js/**", "/img/**", "/images/**",
                "/favicon.ico"
            ).permitAll()

            // 2. 웹 관리자(SUPER_ADMIN) 전용 페이지
            .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")

            // 2-1. 기업 관리자(ADMIN) 전용 페이지: 소속 기업 담당자 승인·관리
            .requestMatchers("/corps/**").hasRole("ADMIN")

            // 3. 기존 REST API: 기록/첨부파일 삭제는 기업 관리자(ADMIN)만 가능
            //    (Controller의 @PreAuthorize와 Service의 기업 소유권 검사로 한 번 더 확인)
            .requestMatchers(HttpMethod.DELETE,
                "/api/inspections/**", "/api/maintenance/**", "/api/media/**").hasRole("ADMIN")

            // 4. 그 외 API는 권한을 부여받은 사용자만 사용 가능
            .requestMatchers("/api/**").hasAnyRole("ADMIN", "INSPECTOR", "SUPER_ADMIN")

            // 5. 그 외 모든 요청은 반드시 로그인을 해야만 접근 가능
            .anyRequest().authenticated()
        );

    // API 요청은 로그인 페이지로 리다이렉트하지 않고 401(미로그인) / 403(권한 없음) 상태 코드로 응답
    http.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
        request -> request.getServletPath().startsWith("/api/")
    ));

    http.authenticationProvider(authenticationProvider());

    http.formLogin(login -> login
        .loginPage("/login")
        .loginProcessingUrl("/users/login")
        .usernameParameter("username")
        .passwordParameter("password")
        .successHandler(loginSuccessHandler())
        .failureHandler(loginFailureHandler())
        .permitAll()
    );

    http.logout(logout -> logout
        .logoutUrl("/users/logout")
        .logoutSuccessUrl("/login?logout=true")
        .invalidateHttpSession(true)
        .deleteCookies("JSESSIONID")
    );

    return http.build();
    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // 비밀번호 확인이 끝난 뒤 사용자/기업 승인 상태를 검사한다.
        provider.setPostAuthenticationChecks(new LoginStatusChecker());
        return provider;
    }

    /**
     * 로그인 성공 시 이동 화면: 웹 관리자(SUPER_ADMIN)는 관리자 대시보드, 그 외는 기존과 동일하게 교량 목록.
     * (기존 defaultSuccessUrl("/bridges", true)와 같이 이전 요청 URL은 사용하지 않는다.)
     */
    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            boolean superAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
            response.sendRedirect(request.getContextPath() + (superAdmin ? "/admin" : "/bridges"));
        };
    }

    /**
     * 로그인 실패 시 원인별 안내 코드로 이동한다.
     * - 승인 상태 문제(AccountStatusException): CORP_PENDING, USER_PENDING 등
     * - 그 외(아이디/비밀번호 불일치): true → 원인을 구분하지 않는 일반 메시지
     */
    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            String code = "true";
            if (exception instanceof AccountStatusException
                    && exception.getMessage() != null
                    && exception.getMessage().matches("(CORP|USER)_[A-Z]+")) {
                code = exception.getMessage();
            }
            response.sendRedirect(request.getContextPath() + "/login?error=" + code);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}