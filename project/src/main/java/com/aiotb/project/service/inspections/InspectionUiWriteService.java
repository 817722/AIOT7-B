package com.aiotb.project.service.inspections;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 점검 등록·수정 화면 보조 조회 서비스.
 */
@Service
@RequiredArgsConstructor
public class InspectionUiWriteService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public record CurrentUser(long no, long corpNo) {
    }

    /** 로그인 사용자 확인: 사용자·소속 기업이 모두 승인(ACTIVE) 상태여야 한다. */
    private CurrentUser actor(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        User user = users.findByUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("사용자가 존재하지 않습니다."));
        if (!user.canUseService()) {
            throw new AccessDeniedException("인증된 기업 사용자만 이용할 수 있습니다.");
        }
        return new CurrentUser(user.getNo(), user.getCorp().getNo());
    }

    /** 점검 담당자 선택 목록: 로그인 사용자와 같은 기업의 활성 사용자만 반환한다. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> managers(Authentication auth) {
        CurrentUser actor = actor(auth);
        return jdbc.query("""
                SELECT no, manager_name
                  FROM users WHERE corp_no = ? AND status = 'ACTIVE'
                 ORDER BY manager_name, no
                """, (rs, idx) -> Map.of("no", rs.getLong("no"), "name", rs.getString("manager_name")), actor.corpNo());
    }
}
