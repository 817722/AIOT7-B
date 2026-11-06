package com.aiotb.project.service.maintenance;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * 유지보수 등록·수정 화면용 기업별 쓰기 어댑터.
 * 기존 팀원의 MaintenanceController/Service 구현을 덮어쓰지 않습니다.
 * DDL-ALOHA(3): maintenance_records, bridges, users, user_auth.
 */
@Service
@RequiredArgsConstructor
public class MaintenanceUiWriteService {
    private static final Set<String> VALID_TYPES = Set.of(
            "균열 보수기록", "단면 복구기록", "구조 보강기록", "하자보수 관리", "기타");
    private static final Set<String> VALID_STATUSES = Set.of("진행 대기", "진행 중", "완료");
    private final JdbcTemplate jdbc;

    public record Payload(Long bridgeNo, String title, String maintenanceType,
                          String memo, LocalDate maintenanceDate, Long managerNo,
                          String status, String result) { }

    private record Actor(long userNo, long corpNo, boolean admin) { }

    private Actor actor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        List<Map<String, Object>> found = jdbc.queryForList("""
                SELECT u.no, u.corp_no
                  FROM users u
                  JOIN corps c ON c.no = u.corp_no AND c.bis_verified = 1
                 WHERE u.username = ? AND u.enabled = 1
                """, authentication.getName());
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "활성화된 기업 사용자만 이용할 수 있습니다.");
        }
        long userNo = ((Number) found.get(0).get("no")).longValue();
        long corpNo = ((Number) found.get(0).get("corp_no")).longValue();
        Integer adminCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_auth
                 WHERE user_no = ? AND auth IN ('ADMIN', 'SUPER_ADMIN')
                """, Integer.class, userNo);
        return new Actor(userNo, corpNo, adminCount != null && adminCount > 0);
    }

    /** 동일 기업에서 등록한 업무용 교량만 반환합니다. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> bridges(Authentication auth, String keyword) {
        Actor actor = actor(auth);
        String word = keyword == null ? "" : keyword.strip();
        if (word.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어는 100자 이하로 입력해 주세요.");
        }
        String escaped = word.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return jdbc.query("""
                SELECT no, bridge_name, province, city, facility_type
                  FROM bridges
                 WHERE corp_no = ?
                   AND (? = '' OR bridge_name LIKE ? ESCAPE '!' OR CAST(no AS CHAR) = ?)
                 ORDER BY bridge_name, no
                 LIMIT 50
                """, (rs, index) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("no", rs.getLong("no"));
            row.put("name", rs.getString("bridge_name"));
            row.put("province", rs.getString("province"));
            row.put("city", rs.getString("city"));
            row.put("facilityType", rs.getString("facility_type"));
            return row;
        }, actor.corpNo(), word, "%" + escaped + "%", word);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> bridge(Authentication auth, long bridgeNo) {
        Actor actor = actor(auth);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT no, bridge_name AS name, province, city, facility_type AS facilityType
                  FROM bridges WHERE no = ? AND corp_no = ?
                """, bridgeNo, actor.corpNo());
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "소속 기업의 교량이 아닙니다.");
        }
        return rows.get(0);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> managers(Authentication auth) {
        Actor actor = actor(auth);
        return jdbc.query("""
                SELECT no, manager_name FROM users
                 WHERE corp_no = ? AND enabled = 1
                 ORDER BY manager_name, no
                """, (rs, index) -> Map.of("no", rs.getLong("no"), "name", rs.getString("manager_name")),
                actor.corpNo());
    }

    /** 참고용 점검 데이터. maintenance_records에는 inspection_no 컬럼이 없어 저장하지 않습니다. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> inspections(Authentication auth, long bridgeNo) {
        Actor actor = actor(auth);
        requireBridge(actor, bridgeNo);
        return jdbc.query("""
                SELECT i.no, i.title, i.status, i.inspection_date
                  FROM inspections i
                 WHERE i.corp_no = ? AND i.bridge_no = ?
                 ORDER BY i.inspection_date DESC, i.no DESC
                 LIMIT 10
                """, (rs, index) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("no", rs.getLong("no"));
            row.put("title", rs.getString("title"));
            row.put("status", rs.getString("status"));
            Date date = rs.getDate("inspection_date");
            row.put("date", date == null ? null : date.toLocalDate());
            return row;
        }, actor.corpNo(), bridgeNo);
    }

    @Transactional(readOnly = true)
    public boolean canEdit(Authentication auth, long maintenanceNo) {
        Actor actor = actor(auth);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT manager_no, created_by FROM maintenance_records
                 WHERE no = ? AND corp_no = ?
                """, maintenanceNo, actor.corpNo());
        if (rows.isEmpty()) return false;
        return permitted(actor, rows.get(0));
    }

    @Transactional
    public long create(Authentication auth, Payload payload) {
        Actor actor = actor(auth);
        validate(payload, false);
        requireBridge(actor, payload.bridgeNo());
        requireManager(actor, payload.managerNo());
        String uuid = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO maintenance_records
                    (id, bridge_no, corp_no, title, maintenance_type, memo,
                     status, maintenance_date, manager_no, result, completed_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        CASE WHEN ? = '완료' THEN NOW() ELSE NULL END, ?)
                """, uuid, payload.bridgeNo(), actor.corpNo(), payload.title().strip(),
                payload.maintenanceType(), trimNullable(payload.memo()), payload.status(),
                Date.valueOf(payload.maintenanceDate()), payload.managerNo(),
                trimNullable(payload.result()), payload.status(), actor.userNo());
        Long newNo = jdbc.queryForObject("""
                SELECT no FROM maintenance_records WHERE id = ? AND corp_no = ?
                """, Long.class, uuid, actor.corpNo());
        if (newNo == null) throw new IllegalStateException("저장된 유지보수 번호를 찾지 못했습니다.");
        return newNo;
    }

    @Transactional
    public void update(Authentication auth, long maintenanceNo, Payload payload) {
        Actor actor = actor(auth);
        validate(payload, true);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT bridge_no, manager_no, created_by FROM maintenance_records
                 WHERE no = ? AND corp_no = ? FOR UPDATE
                """, maintenanceNo, actor.corpNo());
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "유지보수 기록을 찾을 수 없습니다.");
        }
        Map<String, Object> existing = rows.get(0);
        if (!permitted(actor, existing)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "기록 작성자, 담당자 또는 관리자만 수정할 수 있습니다.");
        }
        long existingBridgeNo = ((Number) existing.get("bridge_no")).longValue();
        if (payload.bridgeNo() != null && payload.bridgeNo() != existingBridgeNo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기존 유지보수의 대상 교량은 변경할 수 없습니다.");
        }
        requireManager(actor, payload.managerNo());
        jdbc.update("""
                UPDATE maintenance_records
                   SET title = ?, maintenance_type = ?, memo = ?,
                       maintenance_date = ?, manager_no = ?, status = ?, result = ?,
                       completed_at = CASE WHEN ? = '완료'
                                           THEN COALESCE(completed_at, NOW()) ELSE NULL END,
                       updated_by = ?
                 WHERE no = ? AND corp_no = ?
                """, payload.title().strip(), payload.maintenanceType(),
                trimNullable(payload.memo()), Date.valueOf(payload.maintenanceDate()),
                payload.managerNo(), payload.status(), trimNullable(payload.result()),
                payload.status(), actor.userNo(), maintenanceNo, actor.corpNo());
    }

    private boolean permitted(Actor actor, Map<String, Object> record) {
        long manager = ((Number) record.get("manager_no")).longValue();
        long creator = ((Number) record.get("created_by")).longValue();
        return actor.admin() || actor.userNo() == manager || actor.userNo() == creator;
    }

    private void requireBridge(Actor actor, long bridgeNo) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM bridges WHERE no = ? AND corp_no = ?
                """, Integer.class, bridgeNo, actor.corpNo());
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 소속 기업 교량을 선택해 주세요.");
        }
    }

    private void requireManager(Actor actor, long managerNo) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM users WHERE no = ? AND corp_no = ? AND enabled = 1
                """, Integer.class, managerNo, actor.corpNo());
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 기업의 활성 작업자를 선택해 주세요.");
        }
    }

    private static void validate(Payload payload, boolean editing) {
        if (payload == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입력 데이터가 없습니다.");
        if (!editing && (payload.bridgeNo() == null || payload.bridgeNo() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대상 교량을 선택해 주세요.");
        }
        if (payload.title() == null || payload.title().isBlank() || payload.title().strip().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기록명을 1~255자로 입력해 주세요.");
        }
        if (!VALID_TYPES.contains(payload.maintenanceType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효한 유지보수 분류를 선택해 주세요.");
        }
        if (!VALID_STATUSES.contains(payload.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효한 진행 상태를 선택해 주세요.");
        }
        if (payload.maintenanceDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유지보수 작업일을 입력해 주세요.");
        }
        if (payload.managerNo() == null || payload.managerNo() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "작업 담당자를 선택해 주세요.");
        }
        if (payload.memo() != null && payload.memo().length() > 20000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메모는 20,000자 이하로 입력해 주세요.");
        }
        if (payload.result() != null && payload.result().length() > 20000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결과는 20,000자 이하로 입력해 주세요.");
        }
    }

    private static String trimNullable(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
