package com.aiotb.project.service.inspections;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 로그인 작업자별로 최근 확인한 업무용 교량을 기록·조회합니다.
 * 기업 격리는 users.corp_no와 inspections.corp_no의 일치 조건으로 강제합니다.
 * DDL: DDL-ALOHA(3).sql (inspection_recent)
 */
@Service
@RequiredArgsConstructor
public class InspectionRecentViewService {
    private final JdbcTemplate jdbc;

    /**
     * 상세 페이지의 기업 소속 검증이 성공한 후 호출합니다.
     * DB에서도 로그인 사용자와 교량의 기업 소속을 재검증합니다.
     * 동일 교량을 다시 보면 행을 늘리지 않고 조회 시각만 갱신합니다.
     */
    @Transactional
    public void recordView(String username, long inspectionNo) {
        if (username == null || username.isBlank() || inspectionNo <= 0) {
            return;
        }
        jdbc.update("""
            INSERT INTO inspection_recent (user_no, inspection_no, corp_no, viewed_at)
            SELECT u.no, b.no, u.corp_no, CURRENT_TIMESTAMP(3)
              FROM users u
              JOIN corps c ON c.no = u.corp_no
              JOIN inspections b ON b.no = ? AND b.corp_no = u.corp_no
             WHERE u.username = ? AND u.status = 'ACTIVE' AND c.status = 'ACTIVE'
            ON DUPLICATE KEY UPDATE viewed_at = CURRENT_TIMESTAMP(3)
            """, inspectionNo, username);
    }

    /** 최근 열람 교량 최대 5개. 사용자·기업별로 분리하며 실제 교량 정보는 최신 DB 값으로 표시합니다. */
    @Transactional(readOnly = true)
    public List<Map<String,Object>> listRecent(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return jdbc.query("""
            SELECT b.no AS inspection_no, b.inspection_name, b.province, b.city,
                   b.facility_type, b.last_inspection_date,
                   manager.manager_name,
                   (SELECT i.status
                      FROM inspections i
                     WHERE i.inspection_no = b.no AND i.corp_no = b.corp_no
                     ORDER BY i.inspection_date DESC, i.no DESC LIMIT 1
                   ) AS inspection_status,
                   rv.viewed_at
              FROM inspection_recent rv
              JOIN users u ON u.no = rv.user_no AND u.corp_no = rv.corp_no
              JOIN corps c ON c.no = u.corp_no
              JOIN inspections b ON b.no = rv.inspection_no AND b.corp_no = rv.corp_no
              LEFT JOIN users manager ON manager.no = b.manager_no
                                     AND manager.corp_no = b.corp_no
             WHERE u.username = ? AND u.status = 'ACTIVE' AND c.status = 'ACTIVE'
             ORDER BY rv.viewed_at DESC, rv.no DESC
             LIMIT 5
            """, (rs, index) -> {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("no", rs.getLong("inspection_no"));
                row.put("name", rs.getString("inspection_name"));
                row.put("province", rs.getString("province"));
                row.put("city", rs.getString("city"));
                String facility = rs.getString("facility_type");
                row.put("facilityType", normalizeFacility(facility));
                row.put("displayCode", "#" + rs.getLong("inspection_no"));
                String managerName = rs.getString("manager_name");
                row.put("manager", managerName == null || managerName.isBlank() ? "미배정" : managerName);
                java.sql.Date lastInspection = rs.getDate("last_inspection_date");
                row.put("lastInspectionDate", lastInspection == null ? null : lastInspection.toLocalDate());
                String inspectionStatus = rs.getString("inspection_status");
                row.put("status", inspectionStatus == null || inspectionStatus.isBlank()
                        ? "점검 기록 없음" : inspectionStatus);
                Timestamp viewedAt = rs.getTimestamp("viewed_at");
                row.put("viewedAt", viewedAt == null ? null : viewedAt.toLocalDateTime());
                return row;
            }, username);
    }

    private static String normalizeFacility(String raw) {
        if (raw == null || raw.isBlank()) return "미분류";
        return switch (raw.trim()) {
            case "01", "1", "제1종" -> "제1종";
            case "02", "2", "제2종" -> "제2종";
            case "03", "3", "제3종" -> "제3종";
            case "99" -> "미분류";
            default -> raw;
        };
    }
}
