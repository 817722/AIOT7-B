package com.aiotb.project.service.maintenance;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * 유지보수 목록·상세 화면을 위한 기업별 읽기 전용 조회.
 * 기존 MaintenanceService / MaintenanceController의 CRUD 구현은 변경하지 않습니다.
 * DDL-ALOHA(3): maintenance_records, bridges, users, media.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceListViewService {
    private final JdbcTemplate jdbc;

    public List<Map<String, Object>> list(String username) {
        validateUsername(username);
        return jdbc.query("""
            SELECT m.no AS maintenance_no, m.id AS maintenance_id,
                   m.bridge_no, m.title, m.maintenance_type, m.memo,
                   m.maintenance_date, m.status, m.manager_no, m.result,
                   b.bridge_name, b.province, b.city, b.facility_type,
                   manager.manager_name,
                   CASE WHEN cover.p_id IS NULL THEN 0 ELSE 1 END AS has_cover
              FROM maintenance_records m
              JOIN bridges b ON b.no = m.bridge_no AND b.corp_no = m.corp_no
              JOIN users actor ON actor.corp_no = m.corp_no
                              AND actor.username = ? AND actor.status = 'ACTIVE'
              LEFT JOIN users manager ON manager.no = m.manager_no
                                     AND manager.corp_no = m.corp_no
              LEFT JOIN (
                 SELECT corp_no, p_id
                   FROM media
                  WHERE p_type = 'BRIDGE' AND type = 'IMAGE' AND is_main = 1
                  GROUP BY corp_no, p_id
              ) cover ON cover.corp_no = m.corp_no AND cover.p_id = m.bridge_no
             ORDER BY m.created_at DESC, m.no DESC
            """, (rs, index) -> toRow(rs), username);
    }

    public Map<String, Object> detail(String username, long maintenanceNo) {
        validateUsername(username);
        List<Map<String, Object>> results = jdbc.query("""
            SELECT m.no AS maintenance_no, m.id AS maintenance_id,
                   m.bridge_no, m.title, m.maintenance_type, m.memo,
                   m.maintenance_date, m.status, m.manager_no, m.result,
                   b.bridge_name, b.province, b.city, b.facility_type,
                   manager.manager_name,
                   CASE WHEN cover.p_id IS NULL THEN 0 ELSE 1 END AS has_cover
              FROM maintenance_records m
              JOIN bridges b ON b.no = m.bridge_no AND b.corp_no = m.corp_no
              JOIN users actor ON actor.corp_no = m.corp_no
                              AND actor.username = ? AND actor.status = 'ACTIVE'
              LEFT JOIN users manager ON manager.no = m.manager_no
                                     AND manager.corp_no = m.corp_no
              LEFT JOIN (
                 SELECT corp_no, p_id
                   FROM media
                  WHERE p_type = 'BRIDGE' AND type = 'IMAGE' AND is_main = 1
                  GROUP BY corp_no, p_id
              ) cover ON cover.corp_no = m.corp_no AND cover.p_id = m.bridge_no
             WHERE m.no = ?
            """, (rs, index) -> toRow(rs), username, maintenanceNo);
        if (results.isEmpty()) {
            // 다른 기업 교량의 존재 여부도 공개하지 않습니다.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "유지보수 기록을 찾을 수 없습니다.");
        }
        return results.get(0);
    }

    public List<Map<String, Object>> managers(String username) {
        validateUsername(username);
        return jdbc.query("""
            SELECT candidate.no, candidate.manager_name
              FROM users candidate
              JOIN users actor ON actor.corp_no = candidate.corp_no
                              AND actor.username = ? AND actor.status = 'ACTIVE'
             WHERE candidate.status = 'ACTIVE'
             ORDER BY candidate.manager_name, candidate.no
            """, (rs, index) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("no", rs.getLong("no"));
                m.put("name", rs.getString("manager_name"));
                return m;
            }, username);
    }

    /** 파일 경로나 공개 URL을 반환하지 않습니다. 실제 재생 API는 별도 권한 검사가 필요합니다. */
    public List<Map<String, Object>> videoMetadata(String username, long maintenanceNo) {
        validateUsername(username);
        return jdbc.query("""
            SELECT f.no, f.origin_name, f.mime_type, f.file_size
              FROM media f
              JOIN maintenance_records m
                ON m.no = f.p_id AND m.corp_no = f.corp_no
              JOIN users actor ON actor.corp_no = m.corp_no
                              AND actor.username = ? AND actor.status = 'ACTIVE'
             WHERE f.p_type = 'MAINTENANCE' AND f.type = 'VIDEO' AND m.no = ?
             ORDER BY f.created_at DESC, f.no DESC
            """, (rs, index) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("no", rs.getLong("no"));
                m.put("name", rs.getString("origin_name"));
                m.put("mimeType", rs.getString("mime_type"));
                m.put("size", rs.getObject("file_size"));
                return m;
            }, username, maintenanceNo);
    }

    private static Map<String, Object> toRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("no", rs.getLong("maintenance_no"));
        row.put("id", rs.getString("maintenance_id"));
        row.put("bridgeNo", rs.getLong("bridge_no"));
        row.put("title", rs.getString("title"));
        row.put("maintenanceType", rs.getString("maintenance_type"));
        row.put("memo", rs.getString("memo"));
        row.put("result", rs.getString("result"));
        row.put("status", rs.getString("status"));
        Date date = rs.getDate("maintenance_date");
        row.put("maintenanceDate", date == null ? null : date.toLocalDate());
        long managerNo = rs.getLong("manager_no");
        row.put("managerNo", rs.wasNull() ? null : managerNo);
        row.put("manager", rs.getString("manager_name") == null ? "미배정" : rs.getString("manager_name"));
        row.put("name", rs.getString("bridge_name"));
        row.put("province", safe(rs.getString("province")));
        row.put("city", safe(rs.getString("city")));
        row.put("facilityType", normalizeFacility(rs.getString("facility_type")));
        row.put("hasRepresentativeImage", rs.getInt("has_cover") == 1);
        return row;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeFacility(String value) {
        if (value == null || value.isBlank()) return "미분류";
        return switch (value.trim()) {
            case "01", "1", "제1종" -> "제1종";
            case "02", "2", "제2종" -> "제2종";
            case "03", "3", "제3종" -> "제3종";
            case "99" -> "미분류";
            default -> value;
        };
    }

    private static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }
}
