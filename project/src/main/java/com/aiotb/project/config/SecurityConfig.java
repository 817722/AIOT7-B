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

import com.aiotb.project.service.users.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/join", "/api/business/verify", "/error",
                        "/css/**", "/js/**", "/img/**", "/images/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated());
                // 공개 페이지, 정적 자원
                .requestMatchers("/", "/css/**", "/js/**", "/img/**", "/images/**").permitAll()
                .requestMatchers("/users/join", "/users/login").permitAll()
                // 로그인 필요 영역
                .requestMatchers("/api/maintenance", "/api/inspections").authenticated()
                .anyRequest().permitAll());

        http.authenticationProvider(authenticationProvider());
        http.formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/users/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/bridges", true)
                .failureUrl("/login?error=true")
                .permitAll());
        http.logout(logout -> logout
                .logoutUrl("/users/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"));
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
