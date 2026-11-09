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

import com.aiotb.project.service.users.CustomUserDetailsService;
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
        .failureUrl("/login?error=true")
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
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}