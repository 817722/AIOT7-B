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

import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.aiotb.project.service.users.CustomUserDetailsService;
import com.aiotb.project.service.users.LoginStatusChecker;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
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

            // 2. 점검, 유지보수, 미디어 API 경로의 permitAll()을 제거하여 인증된 사용자만 접근 가능하도록 변경
            // (.anyRequest().authenticated() 에 의해 자동으로 로그인이 필수가 됩니다)

            // 3. 그 외 모든 요청은 반드시 로그인을 해야만 접근 가능
            .anyRequest().authenticated()
        );

    http.authenticationProvider(authenticationProvider());

    http.formLogin(login -> login
        .loginPage("/login")
        .loginProcessingUrl("/users/login")
        .usernameParameter("username")
        .passwordParameter("password")
        .defaultSuccessUrl("/bridges", true)
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