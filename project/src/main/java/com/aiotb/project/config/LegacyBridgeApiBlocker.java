package com.aiotb.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 임시 보호 장치: 기존 /api/bridges 의 테넌트/권한 검증 누락을 우회할 수 없도록 차단.
 * 팀원이 기존 REST API에 기업 소유권/권한 검증을 구현한 뒤 이 보호 장치를 제거하세요.
 * 신규 UI는 /api/bridge-ui 만 사용합니다.
 */
@Configuration
public class LegacyBridgeApiBlocker implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response, Object handler) throws Exception {
                String path = request.getServletPath();
                if ("GET".equals(request.getMethod()) && "/api/bridges/catalog".equals(path)) {
                    return true; // 기존 메모리 기반 CSV 검색만 허용(로그인은 SecurityConfig에서 요구)
                }
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "기존 교량 API는 소속 기업/권한 검증 보강 전까지 비활성화되었습니다.");
                return false;
            }
        }).addPathPatterns("/api/bridges", "/api/bridges/**");
    }
}
