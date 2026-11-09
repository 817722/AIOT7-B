package com.aiotb.project.service.corps;

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

import com.aiotb.project.service.admin.AdminLabels;
import com.aiotb.project.service.users.CurrentUserService.Actor;

import lombok.RequiredArgsConstructor;

/**
 * 기업 관리자(ADMIN) 전용 서비스.
 * 같은 사업자번호로 가입한 담당자(INSPECTOR)를 기업에 들일지 결정한다: 승인 / 반려 / 비활성 / 재활성.
 *
 * 서버 측 검증
 * - 모든 조회·처리는 로그인한 ADMIN의 corp_no 로 제한 (다른 기업 사용자는 404)
 * - 처리 대상은 INSPECTOR 권한 사용자만 (ADMIN / SUPER_ADMIN 계정 및 본인 계정은 처리 불가)
 * - users.status_by 에는 처리한 ADMIN 번호를 기록 (DB 제약 fk_users_status_by_corp: 같은 기업 사용자만 가능)
 */
@Service
@RequiredArgsConstructor
public class CorpManageService {
    private static final Set<String> USER_STATUSES = Set.of("PENDING", "ACTIVE", "REJECTED", "DISABLED");

    private final JdbcTemplate jdbc;

    /** 담당자 상태 처리 유형과 허용되는 이전 상태 */
    public enum MemberAction {
        APPROVE(Set.of("PENDING", "REJECTED"), "ACTIVE", false, "가입 승인"),
        REJECT(Set.of("PENDING"), "REJECTED", true, "가입 반려"),
        DISABLE(Set.of("ACTIVE"), "DISABLED", true, "비활성"),
        REACTIVATE(Set.of("DISABLED"), "ACTIVE", false, "재활성");

        final Set<String> from;
        final String to;
        final boolean reasonRequired;
        public final String label;

        MemberAction(Set<String> from, String to, boolean reasonRequired, String label) {
            this.from = from; this.to = to; this.reasonRequired = reasonRequired; this.label = label;
        }
    }

    private static void requireAdmin(Actor actor) {
        // URL 규칙(/corps/**)과 @PreAuthorize 외에 서비스에서도 한 번 더 확인
        if (actor == null || !actor.admin()) throw new AccessDeniedException("기업 관리자만 이용할 수 있습니다.");
    }

    /** 소속 기업 정보 + 현황 */
    @Transactional(readOnly = true)
    public Map<String, Object> corp(Actor actor) {
        requireAdmin(actor);
        return jdbc.queryForObject("""
                SELECT c.no, c.bis_no, c.corp_name, c.ceo_name, c.opened_at, c.status, c.bis_verified,
                       c.verified_at, c.created_at, c.status_at,
                       (SELECT COUNT(*) FROM users u WHERE u.corp_no = c.no) AS user_count,
                       (SELECT COUNT(*) FROM users u WHERE u.corp_no = c.no AND u.status = 'PENDING') AS pending_users,
                       (SELECT COUNT(*) FROM users u WHERE u.corp_no = c.no AND u.status = 'ACTIVE') AS active_users,
                       (SELECT COUNT(*) FROM bridges b WHERE b.corp_no = c.no) AS bridge_count,
                       (SELECT COUNT(*) FROM bridges b WHERE b.corp_no = c.no AND b.manager_no IS NULL) AS unassigned_bridges,
                       (SELECT COUNT(*) FROM inspections i WHERE i.corp_no = c.no) AS inspection_count,
                       (SELECT COUNT(*) FROM maintenance_records m WHERE m.corp_no = c.no) AS maintenance_count
                  FROM corps c WHERE c.no = ?
                """, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("no", rs.getLong("no"));
            row.put("bisNo", AdminLabels.bisNo(rs.getString("bis_no")));
            row.put("name", rs.getString("corp_name"));
            row.put("ceoName", rs.getString("ceo_name"));
            row.put("openedAt", rs.getDate("opened_at") == null ? null : rs.getDate("opened_at").toLocalDate());
            row.put("status", rs.getString("status"));
            row.put("statusLabel", AdminLabels.corpStatus(rs.getString("status")));
            row.put("verified", rs.getBoolean("bis_verified"));
            row.put("verifiedAt", time(rs, "verified_at"));
            row.put("createdAt", time(rs, "created_at"));
            row.put("statusAt", time(rs, "status_at"));
            row.put("userCount", rs.getLong("user_count"));
            row.put("pendingUsers", rs.getLong("pending_users"));
            row.put("activeUsers", rs.getLong("active_users"));
            row.put("bridgeCount", rs.getLong("bridge_count"));
            row.put("unassignedBridges", rs.getLong("unassigned_bridges"));
            row.put("inspectionCount", rs.getLong("inspection_count"));
            row.put("maintenanceCount", rs.getLong("maintenance_count"));
            return row;
        }, actor.corpNo());
    }

    /** 상태별 담당자 수. 키: ALL, PENDING, ACTIVE, REJECTED, DISABLED */
    @Transactional(readOnly = true)
    public Map<String, Long> memberCounts(Actor actor) {
        requireAdmin(actor);
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", 0L);
        USER_STATUSES.forEach(s -> counts.put(s, 0L));
        jdbc.query("SELECT status, COUNT(*) AS cnt FROM users WHERE corp_no = ? GROUP BY status", rs -> {
            counts.put(rs.getString("status"), rs.getLong("cnt"));
            counts.merge("ALL", rs.getLong("cnt"), Long::sum);
        }, actor.corpNo());
        return counts;
    }

    /** 소속 기업 사용자 목록 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> members(Actor actor, String status, String keyword) {
        requireAdmin(actor);
        String st = USER_STATUSES.contains(status) ? status : "";
        String kw = keyword == null ? "" : keyword.strip();
        if (kw.length() > 100) throw new IllegalArgumentException("검색어는 100자 이하로 입력해 주세요.");
        String like = "%" + kw.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return jdbc.query("""
                SELECT u.no, u.username, u.manager_name, u.phone, u.status, u.created_at, u.status_at, u.reject,
                       (SELECT GROUP_CONCAT(a.auth) FROM user_auth a WHERE a.user_no = u.no) AS auths
                  FROM users u
                 WHERE u.corp_no = ?
                   AND (? = '' OR u.status = ?)
                   AND (? = '' OR u.username LIKE ? ESCAPE '!' OR u.manager_name LIKE ? ESCAPE '!')
                 ORDER BY FIELD(u.status, 'PENDING', 'ACTIVE', 'DISABLED', 'REJECTED'), u.created_at DESC, u.no DESC
                """, (rs, i) -> memberRow(rs, actor), actor.corpNo(), st, st, kw, like, like);
    }

    /** 소속 기업 사용자 상세 (다른 기업 사용자는 404) */
    @Transactional(readOnly = true)
    public Map<String, Object> member(Actor actor, long userNo) {
        requireAdmin(actor);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT u.no, u.username, u.manager_name, u.phone, u.status, u.created_at, u.status_at, u.reject,
                       sb.manager_name AS status_by_name,
                       (SELECT GROUP_CONCAT(a.auth) FROM user_auth a WHERE a.user_no = u.no) AS auths,
                       (SELECT COUNT(*) FROM bridges b WHERE b.manager_no = u.no AND b.corp_no = u.corp_no) AS managed_bridges,
                       (SELECT COUNT(*) FROM inspections i WHERE i.inspector_no = u.no AND i.corp_no = u.corp_no) AS inspection_count,
                       (SELECT COUNT(*) FROM maintenance_records m WHERE m.manager_no = u.no AND m.corp_no = u.corp_no) AS maintenance_count
                  FROM users u
                  LEFT JOIN users sb ON sb.no = u.status_by
                 WHERE u.no = ? AND u.corp_no = ?
                """, (rs, i) -> {
            Map<String, Object> row = memberRow(rs, actor);
            row.put("statusByName", rs.getString("status_by_name"));
            row.put("managedBridges", rs.getLong("managed_bridges"));
            row.put("inspectionCount", rs.getLong("inspection_count"));
            row.put("maintenanceCount", rs.getLong("maintenance_count"));
            return row;
        }, userNo, actor.corpNo());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "소속 기업의 사용자를 찾을 수 없습니다.");
        return rows.get(0);
    }

    /** 담당자 상태 처리: 승인 / 반려 / 비활성 / 재활성 */
    @Transactional
    public void changeMemberStatus(Actor actor, long userNo, MemberAction action, String reason) {
        requireAdmin(actor);
        if (action == null) throw new IllegalArgumentException("처리 유형을 선택해 주세요.");
        String text = reason == null ? "" : reason.strip();
        if (action.reasonRequired && text.isEmpty()) throw new IllegalArgumentException(action.label + " 사유를 입력해 주세요.");
        if (text.length() > 500) throw new IllegalArgumentException("사유는 500자 이하로 입력해 주세요.");

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.status, (SELECT GROUP_CONCAT(a.auth) FROM user_auth a WHERE a.user_no = u.no) AS auths
                  FROM users u WHERE u.no = ? AND u.corp_no = ? FOR UPDATE
                """, userNo, actor.corpNo());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "소속 기업의 사용자를 찾을 수 없습니다.");
        if (userNo == actor.userNo()) throw new AccessDeniedException("본인 계정의 상태는 변경할 수 없습니다.");
        String auths = (String) rows.get(0).get("auths");
        if (!"INSPECTOR".equals(AdminLabels.primaryRole(auths))) {
            throw new AccessDeniedException("담당자(INSPECTOR) 계정만 처리할 수 있습니다.");
        }
        String previous = (String) rows.get(0).get("status");
        if (!action.from.contains(previous)) {
            throw new IllegalArgumentException("현재 상태(" + AdminLabels.userStatus(previous) + ")에서는 "
                    + action.label + " 처리를 할 수 없습니다.");
        }
        int updated = jdbc.update("""
                UPDATE users SET status = ?, status_by = ?, status_at = NOW(), reject = ?
                 WHERE no = ? AND corp_no = ? AND status = ?
                """, action.to, actor.userNo(), action.reasonRequired ? text : null, userNo, actor.corpNo(), previous);
        if (updated != 1) throw new IllegalArgumentException("다른 관리자가 먼저 처리했습니다. 화면을 새로고침해 주세요.");
    }

    private static Map<String, Object> memberRow(ResultSet rs, Actor actor) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        long no = rs.getLong("no");
        String role = AdminLabels.primaryRole(rs.getString("auths"));
        String status = rs.getString("status");
        row.put("no", no);
        row.put("username", rs.getString("username"));
        row.put("name", rs.getString("manager_name"));
        row.put("phone", rs.getString("phone"));
        row.put("status", status);
        row.put("statusLabel", AdminLabels.userStatus(status));
        row.put("role", role);
        row.put("roleLabel", AdminLabels.role(role));
        row.put("reject", rs.getString("reject"));
        row.put("createdAt", time(rs, "created_at"));
        row.put("statusAt", time(rs, "status_at"));
        row.put("self", no == actor.userNo());
        // 화면 표시용 가능한 처리 목록 (실제 검증은 changeMemberStatus에서 다시 수행)
        List<String> actions = new ArrayList<>();
        if ("INSPECTOR".equals(role) && no != actor.userNo()) {
            for (MemberAction a : MemberAction.values()) if (a.from.contains(status)) actions.add(a.name());
        }
        row.put("actions", actions);
        return row;
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }
}
