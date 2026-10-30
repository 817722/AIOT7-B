package com.aiotb.project.service.bridges;

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
 * 현재 사용 중인 DDL-ALOHA(1).sql 의 bridges/inspections/maintenance_records 구조를 대상으로
 * 사용자 소속 기업의 데이터만 조회하는 UI 전용 read-only 어댑터입니다.
 * 기존 BridgeService / BridgeController 의 쓰기 API를 변경하지 않습니다.
 * 주의: 실제 MySQL 테이블이 DDL-ALOHA(1).sql과 일치하는지 읽기 전용으로 확인한 다음 사용해야 합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BridgeListViewService {
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    private static final String BRIDGE_SQL = """
        SELECT b.no, b.id, b.bridge_name AS bridge_name, b.manager_no,
               b.province, b.city, b.facility_type, b.address,
               b.road_name, b.last_inspection_date, b.created_at,
               manager.manager_name AS manager_name,
               (SELECT 1 FROM media img WHERE img.p_type='BRIDGE' AND img.p_id=b.no
                     AND img.corp_no=b.corp_no AND img.is_main=1 LIMIT 1) AS has_main_image,
               (SELECT i.status FROM inspections i
                WHERE i.bridge_no = b.no AND i.corp_no = b.corp_no
                ORDER BY i.inspection_date DESC, i.no DESC LIMIT 1) AS inspection_status
          FROM bridges b
          LEFT JOIN users manager ON manager.no = b.manager_no
                                     AND manager.corp_no = b.corp_no
         WHERE b.corp_no = ?
        """;

    private long currentCorpNo(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AccessDeniedException("사용자를 찾을 수 없습니다."));
        if (!user.isEnabled() || user.getCorp() == null || !user.getCorp().isVerified()) {
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

    private List<Map<String, Object>> findBridges(long corpNo, Long no) {
        String sql = BRIDGE_SQL + (no == null ? " ORDER BY b.no DESC" : " AND b.no = ?");
        Object[] args = no == null ? new Object[] {corpNo} : new Object[] {corpNo, no};
        return jdbcTemplate.query(sql, (rs, index) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            long number = rs.getLong("no");
            Date lastDate = rs.getDate("last_inspection_date");
            String status = rs.getString("inspection_status");
            String manager = rs.getString("manager_name");
            row.put("no", number);
            row.put("representativeImageUrl", rs.getObject("has_main_image") == null
                    ? null : "/api/bridge-ui/" + number + "/image");
            row.put("id", rs.getString("id"));
            row.put("code", String.valueOf(number));
            row.put("displayCode", "#" + number);
            row.put("name", nonNull(rs.getString("bridge_name")));
            row.put("province", nonNull(rs.getString("province")));
            row.put("city", nonNull(rs.getString("city")));
            row.put("facilityType", normalizeFacility(rs.getString("facility_type")));
            row.put("address", nonNull(rs.getString("address")));
            row.put("roadName", nonNull(rs.getString("road_name")));
            row.put("manager", manager == null || manager.isBlank() ? "미배정" : manager);
            row.put("managerNo", rs.getObject("manager_no"));
            row.put("lastInspectionDate", lastDate == null ? null : lastDate.toLocalDate());
            row.put("inspectionStatus", status == null ? "점검 기록 없음" : status);
            row.put("status", status == null ? "점검 기록 없음" : status);
            return row;
        }, args);
    }

    public List<Map<String, Object>> list(String username) {
        return findBridges(currentCorpNo(username), null);
    }

    public Map<String, Object> detail(String username, long no) {
        long corpNo = currentCorpNo(username);
        List<Map<String, Object>> rows = findBridges(corpNo, no);
        if (rows.isEmpty()) {
            // 다른 기업의 교량도 404로 처리해 소속 여부를 노출하지 않습니다.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "교량을 찾을 수 없습니다.");
        }
        return rows.get(0);
    }

    public List<Map<String, Object>> inspections(String username, long bridgeNo) {
        long corpNo = currentCorpNo(username);
        // 먼저 기업 소유 여부를 검사합니다.
        detail(username, bridgeNo);
        return jdbcTemplate.query("""
            SELECT i.no, i.title, i.inspection_type, i.inspection_date, i.status,
                   inspector.manager_name
              FROM inspections i
              LEFT JOIN users inspector ON inspector.no=i.inspector_no AND inspector.corp_no=i.corp_no
             WHERE i.corp_no=? AND i.bridge_no=?
             ORDER BY i.inspection_date DESC, i.no DESC
            """, (rs, index) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            long no = rs.getLong("no");
            item.put("displayCode", "#" + no);
            item.put("type", nonNull(rs.getString("inspection_type")));
            Date d = rs.getDate("inspection_date");
            item.put("date", d == null ? null : d.toLocalDate());
            item.put("inspector", nonNull(rs.getString("manager_name")));
            item.put("status", nonNull(rs.getString("status")));
            item.put("description", nonNull(rs.getString("title")));
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
              LEFT JOIN users manager ON manager.no=m.manager_no AND manager.corp_no=m.corp_no
             WHERE m.corp_no=? AND m.bridge_no=?
             ORDER BY m.maintenance_date DESC, m.no DESC
            """, (rs, index) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            long no = rs.getLong("no");
            item.put("displayCode", "#" + no);
            item.put("type", nonNull(rs.getString("maintenance_type")));
            Date d = rs.getDate("maintenance_date");
            item.put("date", d == null ? null : d.toLocalDate());
            item.put("manager", nonNull(rs.getString("manager_name")));
            item.put("status", nonNull(rs.getString("status")));
            item.put("description", nonNull(rs.getString("title")));
            item.put("category", "유지보수");
            return item;
        }, corpNo, bridgeNo);
    }

    public List<Map<String, Object>> recentWork(List<Map<String,Object>> inspections,
                                                   List<Map<String,Object>> maintenances) {
        List<Map<String,Object>> list = new ArrayList<>();
        list.addAll(inspections);
        list.addAll(maintenances);
        list.sort(Comparator.comparing(
            (Map<String,Object> row) -> (LocalDate) row.get("date"),
            Comparator.nullsLast(Comparator.reverseOrder())));
        return list.stream().limit(5).toList();
    }
}
