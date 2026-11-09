package com.aiotb.project.service.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.service.users.CurrentUserService.Actor;

import lombok.RequiredArgsConstructor;

/**
 * 웹 관리자(SUPER_ADMIN) 전용 서비스.
 * - 전체 기업/사용자 조회, 시스템 현황
 * - 기업 가입 승인·반려, 이용 정지·해제 (corps.status / status_by / status_at / reject)
 * - 기업 상태 처리 이력(corp_status_history) 기록·조회 (테이블이 없으면 건너뜀)
 *
 * 사용자(users.status) 승인은 같은 기업 ADMIN의 업무이며,
 * DB의 fk_users_status_by_corp 제약상 SUPER_ADMIN은 다른 기업 사용자의 상태를 처리할 수 없으므로 조회만 제공한다.
 */
@Service
@RequiredArgsConstructor
public class AdminService {
    public static final int PAGE_SIZE = 15;
    private static final Set<String> CORP_STATUSES = Set.of("PENDING", "ACTIVE", "REJECTED", "SUSPENDED");
    private static final Set<String> USER_STATUSES = Set.of("PENDING", "ACTIVE", "REJECTED", "DISABLED");
    private static final Set<String> ROLES = Set.of("SUPER_ADMIN", "ADMIN", "INSPECTOR");

    private final JdbcTemplate jdbc;

    /** 기업 상태 처리 유형과 허용되는 이전 상태 */
    public enum CorpAction {
        APPROVE(Set.of("PENDING", "REJECTED"), "ACTIVE", false),
        REJECT(Set.of("PENDING"), "REJECTED", true),
        SUSPEND(Set.of("ACTIVE"), "SUSPENDED", true),
        RESTORE(Set.of("SUSPENDED"), "ACTIVE", false);

        final Set<String> from;
        final String to;
        final boolean reasonRequired;

        CorpAction(Set<String> from, String to, boolean reasonRequired) {
            this.from = from; this.to = to; this.reasonRequired = reasonRequired;
        }
    }

    public record Page(List<Map<String, Object>> rows, int pageNo, int totalPages, long total) {
        public boolean hasPrev() { return pageNo > 1; }
        public boolean hasNext() { return pageNo < totalPages; }
    }

    private static void requireSuperAdmin(Actor actor) {
        // URL 규칙(/admin/**)과 @PreAuthorize 외에 서비스에서도 한 번 더 확인
        if (actor == null || !actor.superAdmin()) throw new AccessDeniedException("웹 관리자만 이용할 수 있습니다.");
    }

    // ───────────────────────── 시스템 현황 ─────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(Actor actor) {
        requireSuperAdmin(actor);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("corpCounts", corpCounts(""));
        stats.put("userTotal", count("SELECT COUNT(*) FROM users"));
        stats.put("userPending", count("SELECT COUNT(*) FROM users WHERE status = 'PENDING'"));
        stats.put("bridgeTotal", count("SELECT COUNT(*) FROM bridges"));
        stats.put("inspectionTotal", count("SELECT COUNT(*) FROM inspections"));
        stats.put("maintenanceTotal", count("SELECT COUNT(*) FROM maintenance_records"));
        stats.put("pendingCorps", corps(actor, "PENDING", "", 1).rows().stream().limit(5).toList());
        stats.put("historyAvailable", historyAvailable());
        stats.put("recentHistory", history(null, 10));
        return stats;
    }

    // ───────────────────────── 기업 ─────────────────────────

    /** 상태별 기업 수 (검색어 반영). 키: ALL, PENDING, ACTIVE, REJECTED, SUSPENDED */
    @Transactional(readOnly = true)
    public Map<String, Long> corpCounts(String keyword) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", 0L);
        CORP_STATUSES.forEach(s -> counts.put(s, 0L));
        String like = like(keyword);
        jdbc.query("""
                SELECT status, COUNT(*) AS cnt FROM corps
                 WHERE (? = '' OR corp_name LIKE ? ESCAPE '!' OR bis_no LIKE ? ESCAPE '!')
                 GROUP BY status
                """, rs -> {
            counts.put(rs.getString("status"), rs.getLong("cnt"));
            counts.merge("ALL", rs.getLong("cnt"), Long::sum);
        }, keyword(keyword), like, likeDigits(keyword));
        return counts;
    }

    @Transactional(readOnly = true)
    public Page corps(Actor actor, String status, String keyword, int page) {
        requireSuperAdmin(actor);
        String st = CORP_STATUSES.contains(status) ? status : "";
        String kw = keyword(keyword);
        String where = """
                 WHERE (? = '' OR c.status = ?)
                   AND (? = '' OR c.corp_name LIKE ? ESCAPE '!' OR c.bis_no LIKE ? ESCAPE '!')
                """;
        Object[] args = { st, st, kw, like(kw), likeDigits(kw) };
        long total = count("SELECT COUNT(*) FROM corps c" + where, args);
        int pages = Math.max(1, (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE));
        int current = Math.max(1, Math.min(page, pages));
        List<Object> listArgs = new ArrayList<>(List.of(args));
        listArgs.add(PAGE_SIZE);
        listArgs.add((current - 1) * PAGE_SIZE);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT c.no, c.bis_no, c.corp_name, c.ceo_name, c.status, c.bis_verified,
                       c.created_at, c.status_at,
                       (SELECT COUNT(*) FROM users u WHERE u.corp_no = c.no) AS user_count,
                       (SELECT COUNT(*) FROM users u WHERE u.corp_no = c.no AND u.status = 'PENDING') AS pending_users,
                       (SELECT COUNT(*) FROM bridges b WHERE b.corp_no = c.no) AS bridge_count,
                       (SELECT u.manager_name FROM users u JOIN user_auth a ON a.user_no = u.no AND a.auth = 'ADMIN'
                         WHERE u.corp_no = c.no ORDER BY u.no LIMIT 1) AS admin_name
                  FROM corps c
                """ + where + " ORDER BY FIELD(c.status,'PENDING','ACTIVE','SUSPENDED','REJECTED'), c.created_at DESC, c.no DESC LIMIT ? OFFSET ?",
                (rs, i) -> corpRow(rs), listArgs.toArray());
        return new Page(rows, current, pages, total);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> corp(Actor actor, long corpNo) {
        requireSuperAdmin(actor);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT c.no, c.bis_no, c.corp_name, c.ceo_name, c.status, c.bis_verified,
                       c.opened_at, c.verified_at, c.created_at, c.status_at, c.reject,
                       sb.manager_name AS status_by_name,
                       (SELECT COUNT(*) FROM users u WHERE u.corp_no = c.no) AS user_count,
                       (SELECT COUNT(*) FROM users u WHERE u.corp_no = c.no AND u.status = 'PENDING') AS pending_users,
                       (SELECT COUNT(*) FROM bridges b WHERE b.corp_no = c.no) AS bridge_count,
                       (SELECT COUNT(*) FROM inspections i WHERE i.corp_no = c.no) AS inspection_count,
                       (SELECT COUNT(*) FROM maintenance_records m WHERE m.corp_no = c.no) AS maintenance_count,
                       (SELECT u.manager_name FROM users u JOIN user_auth a ON a.user_no = u.no AND a.auth = 'ADMIN'
                         WHERE u.corp_no = c.no ORDER BY u.no LIMIT 1) AS admin_name,
                       (SELECT COUNT(*) FROM users u JOIN user_auth a ON a.user_no = u.no AND a.auth = 'SUPER_ADMIN'
                         WHERE u.corp_no = c.no) AS operator_flag
                  FROM corps c
                  LEFT JOIN users sb ON sb.no = c.status_by
                 WHERE c.no = ?
                """, (rs, i) -> {
            Map<String, Object> row = corpRow(rs);
            row.put("openedAt", rs.getDate("opened_at") == null ? null : rs.getDate("opened_at").toLocalDate());
            row.put("verifiedAt", time(rs, "verified_at"));
            row.put("reject", rs.getString("reject"));
            row.put("statusByName", rs.getString("status_by_name"));
            row.put("inspectionCount", rs.getLong("inspection_count"));
            row.put("maintenanceCount", rs.getLong("maintenance_count"));
            boolean operator = rs.getLong("operator_flag") > 0;
            row.put("operator", operator);
            String status = rs.getString("status");
            // 화면에는 현재 상태에서 가능한 처리만 표시한다. (실제 검증은 changeCorpStatus에서 다시 수행)
            List<String> actions = new ArrayList<>();
            if (!operator) {
                for (CorpAction a : CorpAction.values()) if (a.from.contains(status)) actions.add(a.name());
            }
            row.put("actions", actions);
            return row;
        }, corpNo);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "기업을 찾을 수 없습니다.");
        Map<String, Object> corp = rows.get(0);
        corp.put("users", users(actor, corpNo, "", "", "", 1, 200).rows());
        corp.put("historyAvailable", historyAvailable());
        corp.put("history", history(corpNo, 50));
        return corp;
    }

    /**
     * 기업 상태 변경. 허용되지 않은 상태 전환이나 사유 누락은 IllegalArgumentException.
     * 운영사 기업(SUPER_ADMIN 소속)은 변경할 수 없다.
     */
    @Transactional
    public void changeCorpStatus(Actor actor, long corpNo, CorpAction action, String reason) {
        requireSuperAdmin(actor);
        if (action == null) throw new IllegalArgumentException("처리 유형을 선택해 주세요.");
        String text = reason == null ? "" : reason.strip();
        if (action.reasonRequired && text.isEmpty()) {
            throw new IllegalArgumentException(AdminLabels.corpAction(action.name()) + " 사유를 입력해 주세요.");
        }
        if (text.length() > 500) throw new IllegalArgumentException("사유는 500자 이하로 입력해 주세요.");

        List<String> current = jdbc.queryForList("SELECT status FROM corps WHERE no = ? FOR UPDATE", String.class, corpNo);
        if (current.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "기업을 찾을 수 없습니다.");
        Integer operator = jdbc.queryForObject("""
                SELECT COUNT(*) FROM users u JOIN user_auth a ON a.user_no = u.no AND a.auth = 'SUPER_ADMIN'
                 WHERE u.corp_no = ?
                """, Integer.class, corpNo);
        if (operator != null && operator > 0) {
            throw new IllegalArgumentException("운영사 기업의 상태는 변경할 수 없습니다.");
        }
        String previous = current.get(0);
        if (!action.from.contains(previous)) {
            throw new IllegalArgumentException("현재 상태(" + AdminLabels.corpStatus(previous) + ")에서는 "
                    + AdminLabels.corpAction(action.name()) + " 처리를 할 수 없습니다.");
        }
        // 반려·정지 사유는 corps.reject 에 보관, 승인·정지 해제 시 비움 (과거 사유는 이력 테이블에 남음)
        int updated = jdbc.update("""
                UPDATE corps SET status = ?, status_by = ?, status_at = NOW(), reject = ?
                 WHERE no = ? AND status = ?
                """, action.to, actor.userNo(), action.reasonRequired ? text : null, corpNo, previous);
        if (updated != 1) throw new IllegalArgumentException("다른 관리자가 먼저 처리했습니다. 화면을 새로고침해 주세요.");

        if (historyAvailable()) {
            jdbc.update("""
                    INSERT INTO corp_status_history (corp_no, action, previous_status, new_status, reason, changed_by)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, corpNo, action.name(), previous, action.to, text.isEmpty() ? null : text, actor.userNo());
        }
    }

    // ───────────────────────── 사용자 (조회 전용) ─────────────────────────

    @Transactional(readOnly = true)
    public Page users(Actor actor, Long corpNo, String status, String role, String keyword, int page) {
        return users(actor, corpNo, status, role, keyword, page, PAGE_SIZE);
    }

    private Page users(Actor actor, Long corpNo, String status, String role, String keyword, int page, int size) {
        requireSuperAdmin(actor);
        String st = USER_STATUSES.contains(status) ? status : "";
        String rl = ROLES.contains(role) ? role : "";
        String kw = keyword(keyword);
        long corp = corpNo == null ? 0L : corpNo;
        String where = """
                 WHERE (? = 0 OR u.corp_no = ?)
                   AND (? = '' OR u.status = ?)
                   AND (? = '' OR EXISTS (SELECT 1 FROM user_auth ra WHERE ra.user_no = u.no AND ra.auth = ?))
                   AND (? = '' OR u.username LIKE ? ESCAPE '!' OR u.manager_name LIKE ? ESCAPE '!')
                """;
        Object[] args = { corp, corp, st, st, rl, rl, kw, like(kw), like(kw) };
        long total = count("SELECT COUNT(*) FROM users u" + where, args);
        int pages = Math.max(1, (int) ((total + size - 1) / size));
        int current = Math.max(1, Math.min(page, pages));
        List<Object> listArgs = new ArrayList<>(List.of(args));
        listArgs.add(size);
        listArgs.add((current - 1) * size);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT u.no, u.username, u.manager_name, u.phone, u.status, u.created_at, u.status_at,
                       u.corp_no, c.corp_name, c.status AS corp_status,
                       (SELECT GROUP_CONCAT(a.auth) FROM user_auth a WHERE a.user_no = u.no) AS auths
                  FROM users u JOIN corps c ON c.no = u.corp_no
                """ + where + " ORDER BY u.created_at DESC, u.no DESC LIMIT ? OFFSET ?",
                (rs, i) -> userRow(rs), listArgs.toArray());
        return new Page(rows, current, pages, total);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> user(Actor actor, long userNo) {
        requireSuperAdmin(actor);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT u.no, u.username, u.manager_name, u.phone, u.status, u.created_at, u.status_at, u.reject,
                       u.corp_no, c.corp_name, c.status AS corp_status, c.bis_no,
                       sb.manager_name AS status_by_name,
                       (SELECT GROUP_CONCAT(a.auth) FROM user_auth a WHERE a.user_no = u.no) AS auths,
                       (SELECT COUNT(*) FROM bridges b WHERE b.manager_no = u.no) AS managed_bridges,
                       (SELECT COUNT(*) FROM inspections i WHERE i.inspector_no = u.no) AS inspection_count,
                       (SELECT COUNT(*) FROM maintenance_records m WHERE m.manager_no = u.no) AS maintenance_count
                  FROM users u
                  JOIN corps c ON c.no = u.corp_no
                  LEFT JOIN users sb ON sb.no = u.status_by
                 WHERE u.no = ?
                """, (rs, i) -> {
            Map<String, Object> row = userRow(rs);
            row.put("reject", rs.getString("reject"));
            row.put("bisNo", AdminLabels.bisNo(rs.getString("bis_no")));
            row.put("statusByName", rs.getString("status_by_name"));
            row.put("managedBridges", rs.getLong("managed_bridges"));
            row.put("inspectionCount", rs.getLong("inspection_count"));
            row.put("maintenanceCount", rs.getLong("maintenance_count"));
            return row;
        }, userNo);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        return rows.get(0);
    }

    /** 사용자 목록 필터용 기업 목록 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> corpOptions(Actor actor) {
        requireSuperAdmin(actor);
        return jdbc.query("SELECT no, corp_name FROM corps ORDER BY corp_name, no", (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("no", rs.getLong("no"));
            row.put("name", rs.getString("corp_name"));
            return row;
        });
    }

    // ───────────────────────── 처리 이력 ─────────────────────────

    /** 이력 테이블(corp_status_history) 설치 여부. 미설치 시 이력 기록/표시만 건너뛴다. */
    public boolean historyAvailable() {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = 'corp_status_history'
                """, Integer.class);
        return n != null && n > 0;
    }

    private List<Map<String, Object>> history(Long corpNo, int limit) {
        if (!historyAvailable()) return List.of();
        long corp = corpNo == null ? 0L : corpNo;
        return jdbc.query("""
                SELECT h.no, h.corp_no, c.corp_name, h.action, h.previous_status, h.new_status,
                       h.reason, h.changed_at, u.manager_name AS changed_by_name, u.username AS changed_by_username
                  FROM corp_status_history h
                  JOIN corps c ON c.no = h.corp_no
                  JOIN users u ON u.no = h.changed_by
                 WHERE (? = 0 OR h.corp_no = ?)
                 ORDER BY h.changed_at DESC, h.no DESC
                 LIMIT ?
                """, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("corpNo", rs.getLong("corp_no"));
            row.put("corpName", rs.getString("corp_name"));
            row.put("action", rs.getString("action"));
            row.put("actionLabel", AdminLabels.corpAction(rs.getString("action")));
            row.put("previousStatusLabel", AdminLabels.corpStatus(rs.getString("previous_status")));
            row.put("newStatus", rs.getString("new_status"));
            row.put("newStatusLabel", AdminLabels.corpStatus(rs.getString("new_status")));
            row.put("reason", rs.getString("reason"));
            row.put("changedAt", time(rs, "changed_at"));
            row.put("changedBy", rs.getString("changed_by_name") + " (" + rs.getString("changed_by_username") + ")");
            return row;
        }, corp, corp, limit);
    }

    // ───────────────────────── 공통 ─────────────────────────

    private static Map<String, Object> corpRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("no", rs.getLong("no"));
        row.put("bisNo", AdminLabels.bisNo(rs.getString("bis_no")));
        row.put("name", rs.getString("corp_name"));
        row.put("ceoName", rs.getString("ceo_name"));
        row.put("status", rs.getString("status"));
        row.put("statusLabel", AdminLabels.corpStatus(rs.getString("status")));
        row.put("verified", rs.getBoolean("bis_verified"));
        row.put("createdAt", time(rs, "created_at"));
        row.put("statusAt", time(rs, "status_at"));
        row.put("userCount", rs.getLong("user_count"));
        row.put("pendingUsers", rs.getLong("pending_users"));
        row.put("bridgeCount", rs.getLong("bridge_count"));
        row.put("adminName", rs.getString("admin_name"));
        return row;
    }

    private static Map<String, Object> userRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        String role = AdminLabels.primaryRole(rs.getString("auths"));
        row.put("no", rs.getLong("no"));
        row.put("username", rs.getString("username"));
        row.put("name", rs.getString("manager_name"));
        row.put("phone", rs.getString("phone"));
        row.put("status", rs.getString("status"));
        row.put("statusLabel", AdminLabels.userStatus(rs.getString("status")));
        row.put("role", role);
        row.put("roleLabel", AdminLabels.role(role));
        row.put("corpNo", rs.getLong("corp_no"));
        row.put("corpName", rs.getString("corp_name"));
        row.put("corpStatus", rs.getString("corp_status"));
        row.put("corpStatusLabel", AdminLabels.corpStatus(rs.getString("corp_status")));
        row.put("createdAt", time(rs, "created_at"));
        row.put("statusAt", time(rs, "status_at"));
        return row;
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private static String keyword(String keyword) {
        String kw = keyword == null ? "" : keyword.strip();
        if (kw.length() > 100) throw new IllegalArgumentException("검색어는 100자 이하로 입력해 주세요.");
        return kw;
    }

    private static String like(String keyword) {
        String kw = keyword(keyword);
        return "%" + kw.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    /** 사업자번호는 하이픈 없이 저장되므로 숫자만 남겨 검색한다. (숫자가 없으면 매칭되지 않음) */
    private static String likeDigits(String keyword) {
        String digits = keyword(keyword).replaceAll("\\D", "");
        return digits.isEmpty() ? "__no_match__" : "%" + digits + "%";
    }
}
