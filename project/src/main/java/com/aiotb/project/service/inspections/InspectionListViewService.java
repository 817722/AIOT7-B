package com.aiotb.project.service.inspections;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 점검 관리 화면 전용 조회 서비스.
 * DDL-ALOHA.sql의 bridges / inspections / maintenance_records 구조를 사용하며
 * 로그인 기업의 교량 중 실제 점검 기록이 존재하는 교량만 목록에 표시합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionListViewService {
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    private static final String BRIDGE_SELECT = """
        SELECT b.no, b.id, b.bridge_name AS inspection_name, b.manager_no,
               b.province, b.city, b.facility_type, b.address,
               b.road_name, b.created_at,
               manager.manager_name AS manager_name,
               (SELECT i.inspection_date FROM inspections i
                 WHERE i.bridge_no = b.no AND i.corp_no = b.corp_no
                 ORDER BY i.inspection_date DESC, i.no DESC LIMIT 1) AS last_inspection_date,
               (SELECT i.status FROM inspections i
                 WHERE i.bridge_no = b.no AND i.corp_no = b.corp_no
                 ORDER BY i.inspection_date DESC, i.no DESC LIMIT 1) AS inspection_status,
               (SELECT 1 FROM media img
                 WHERE img.p_type = 'BRIDGE' AND img.type = 'IMAGE'
                   AND img.p_id = b.no AND img.corp_no = b.corp_no
                   AND img.is_main = 1 LIMIT 1) AS has_main_image
          FROM bridges b
          LEFT JOIN users manager
            ON manager.no = b.manager_no AND manager.corp_no = b.corp_no
         WHERE b.corp_no = ?
           AND EXISTS (
               SELECT 1 FROM inspections i
                WHERE i.bridge_no = b.no AND i.corp_no = b.corp_no
           )
        """;

    private long currentCorpNo(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AccessDeniedException("사용자를 찾을 수 없습니다."));
        if (!user.canUseService()) {
            throw new AccessDeniedException("기업 인증이 완료된 사용자만 접근할 수 있습니다.");
        }
        return user.getCorp().getNo();
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
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

    private List<Map<String, Object>> findBridges(long corpNo, Long bridgeNo) {
        String sql = BRIDGE_SELECT + (bridgeNo == null
                ? " ORDER BY last_inspection_date DESC, b.no DESC"
                : " AND b.no = ?");
        Object[] args = bridgeNo == null ? new Object[] {corpNo} : new Object[] {corpNo, bridgeNo};

        return jdbcTemplate.query(sql, (rs, index) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            long no = rs.getLong("no");
            Date lastDate = rs.getDate("last_inspection_date");
            String status = rs.getString("inspection_status");
            String manager = rs.getString("manager_name");

            row.put("no", no);
            row.put("id", rs.getString("id"));
            row.put("code", String.valueOf(no));
            row.put("displayCode", "#" + no);
            row.put("name", nonNull(rs.getString("inspection_name")));
            row.put("province", nonNull(rs.getString("province")));
            row.put("city", nonNull(rs.getString("city")));
            row.put("facilityType", normalizeFacility(rs.getString("facility_type")));
            row.put("address", nonNull(rs.getString("address")));
            row.put("roadName", nonNull(rs.getString("road_name")));
            row.put("manager", manager == null || manager.isBlank() ? "미배정" : manager);
            row.put("managerNo", rs.getObject("manager_no"));
            row.put("lastInspectionDate", lastDate == null ? null : lastDate.toLocalDate());
            row.put("inspectionStatus", status);
            row.put("status", status);
            row.put("representativeImageUrl", rs.getObject("has_main_image") == null
                    ? null : "/api/bridge-ui/" + no + "/image");
            return row;
        }, args);
    }

    /** 실제 점검 기록이 한 건 이상 있는 교량만 반환합니다. */
    public List<Map<String, Object>> list(String username) {
        return findBridges(currentCorpNo(username), null);
    }

    /** 점검 목록에서 선택한 교량의 상세 정보를 반환합니다. */
    public Map<String, Object> detail(String username, long bridgeNo) {
        long corpNo = currentCorpNo(username);
        List<Map<String, Object>> rows = findBridges(corpNo, bridgeNo);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "점검 기록이 있는 교량을 찾을 수 없습니다.");
        }
        return rows.get(0);
    }

    public List<Map<String, Object>> inspections(String username, long bridgeNo) {
        long corpNo = currentCorpNo(username);
        detail(username, bridgeNo);
        return jdbcTemplate.query("""
            SELECT i.no, i.title, i.inspection_type, i.inspection_date, i.status,
                   i.memo, i.overall_result, i.review_opinion,
                   inspector.manager_name
              FROM inspections i
              LEFT JOIN users inspector
                ON inspector.no = i.inspector_no AND inspector.corp_no = i.corp_no
             WHERE i.corp_no = ? AND i.bridge_no = ?
             ORDER BY i.inspection_date DESC, i.no DESC
            """, (rs, index) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("no", rs.getLong("no"));
            item.put("displayCode", "#" + rs.getLong("no"));
            item.put("type", nonNull(rs.getString("inspection_type")));
            Date date = rs.getDate("inspection_date");
            item.put("date", date == null ? null : date.toLocalDate());
            item.put("inspector", nonNull(rs.getString("manager_name")));
            item.put("status", nonNull(rs.getString("status")));
            item.put("description", nonNull(rs.getString("title")));
            item.put("memo", nonNull(rs.getString("memo")));
            item.put("overallResult", nonNull(rs.getString("overall_result")));
            item.put("reviewOpinion", nonNull(rs.getString("review_opinion")));
            item.put("category", "점검");
            return item;
        }, corpNo, bridgeNo);
    }

    public List<Map<String, Object>> maintenances(String username, long bridgeNo) {
        long corpNo = currentCorpNo(username);
        detail(username, bridgeNo);
        return jdbcTemplate.query("""
            SELECT m.no, m.title, m.maintenance_type, m.maintenance_date, m.status,
                   manager.manager_name
              FROM maintenance_records m
              LEFT JOIN users manager
                ON manager.no = m.manager_no AND manager.corp_no = m.corp_no
             WHERE m.corp_no = ? AND m.bridge_no = ?
             ORDER BY m.maintenance_date DESC, m.no DESC
            """, (rs, index) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("no", rs.getLong("no"));
            item.put("displayCode", "#" + rs.getLong("no"));
            item.put("type", nonNull(rs.getString("maintenance_type")));
            Date date = rs.getDate("maintenance_date");
            item.put("date", date == null ? null : date.toLocalDate());
            item.put("manager", nonNull(rs.getString("manager_name")));
            item.put("status", nonNull(rs.getString("status")));
            item.put("description", nonNull(rs.getString("title")));
            item.put("category", "유지보수");
            return item;
        }, corpNo, bridgeNo);
    }

    public List<Map<String, Object>> recentWork(List<Map<String, Object>> inspections,
                                                 List<Map<String, Object>> maintenances) {
        List<Map<String, Object>> work = new ArrayList<>();
        work.addAll(inspections);
        work.addAll(maintenances);
        work.sort(Comparator.comparing(
            (Map<String, Object> row) -> (LocalDate) row.get("date"),
            Comparator.nullsLast(Comparator.reverseOrder())));
        return work.stream().limit(5).toList();
    }
}
