package com.aiotb.project.service.inspections;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * DDL-ALOHA(2).sql 업무용 교량 CRUD 어댑터. 팀원 기존 /api/inspections REST 코드를 변경하지 않음.
 * inspection_master의 실제 DB PK(no)만 입력받고 교량 속성은 DB의 원본에서 복사한다.
 */
@Service
@RequiredArgsConstructor
public class InspectionUiWriteService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public record CurrentUser(long no, long corpNo) {}

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

    private void requireManager(Authentication auth) {
        boolean allowed = auth != null && auth.getAuthorities().stream().anyMatch(a ->
            "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (!allowed) throw new AccessDeniedException("교량 변경 및 삭제는 관리자 권한이 필요합니다.");
    }

    /**
     * 전국교량 원본 검색 결과에 현재 로그인 기업의 등록 상태를 추가한다.
     * b.corp_no 를 JOIN 조건에서 제한해 타 기업의 등록 교량 번호/담당자가 노출되지 않게 한다.
     * 기존 catalogId, masterNo 등 응답 필드는 유지한다.
     */
    @Transactional(readOnly = true)
    public List<Map<String,Object>> catalog(String keyword, Authentication auth) {
        CurrentUser actor = actor(auth);
        if (keyword == null || keyword.isBlank()) return List.of();
        String word = keyword.strip();
        if (word.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어가 너무 깁니다.");
        }
        // LIKE 와일드카드 이스케이프
        String escaped = word.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return jdbc.query("""
            SELECT bm.no, bm.name, bm.facility_type, bm.road_type, bm.road_name,
                   bm.province, bm.city, bm.address, bm.inspection_length, bm.inspection_width,
                   bm.management_agency, bm.source_data_date,
                   b.no AS registered_inspection_no,
                   manager.manager_name AS current_manager_name
              FROM inspection_master bm
              LEFT JOIN inspections b
                ON b.inspection_master_no = bm.no AND b.corp_no = ?
              LEFT JOIN users manager
                ON manager.no = b.manager_no AND manager.corp_no = b.corp_no
             WHERE bm.name LIKE ? ESCAPE '!'
             ORDER BY bm.name, bm.province, bm.city, bm.no
             LIMIT 50
            """, (rs, idx) -> {
            Map<String,Object> item = new LinkedHashMap<>();
            long masterNo = rs.getLong("no");
            long foundInspectionNo = rs.getLong("registered_inspection_no");
            Long registeredInspectionNo = rs.wasNull() ? null : foundInspectionNo;
            String currentManager = rs.getString("current_manager_name");

            item.put("catalogId", String.valueOf(masterNo));
            item.put("masterNo", masterNo);
            item.put("name", rs.getString("name"));
            item.put("facilityType", rs.getString("facility_type"));
            item.put("roadType", rs.getString("road_type"));
            item.put("roadName", rs.getString("road_name"));
            item.put("province", rs.getString("province"));
            item.put("city", rs.getString("city"));
            item.put("address", rs.getString("address"));
            item.put("inspectionLength", rs.getBigDecimal("inspection_length"));
            item.put("inspectionWidth", rs.getBigDecimal("inspection_width"));
            item.put("managementAgency", rs.getString("management_agency"));
            java.sql.Date date = rs.getDate("source_data_date");
            item.put("sourceDataDate", date == null ? null : date.toLocalDate());
            item.put("registered", registeredInspectionNo != null);
            item.put("registeredInspectionNo", registeredInspectionNo);
            item.put("currentManagerName", registeredInspectionNo == null ? null :
                (currentManager == null || currentManager.isBlank() ? "미지정" : currentManager));
            return item;
        }, actor.corpNo(), "%" + escaped + "%");
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> managers(Authentication auth) {
        CurrentUser actor = actor(auth);
        return jdbc.query("""
            SELECT no, manager_name
              FROM users WHERE corp_no = ? AND status = 'ACTIVE'
             ORDER BY manager_name, no
            """, (rs, idx) -> Map.of("no", rs.getLong("no"), "name", rs.getString("manager_name")), actor.corpNo());
    }

    /** 교량 신규 등록: 원본 컬럼은 클라이언트가 아닌 inspection_master에서 복사한다. */
    @Transactional
    public long create(long masterNo, Authentication auth) {
        CurrentUser actor = actor(auth);
        if (masterNo <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "원본 교량을 선택해 주세요.");
        String id = UUID.randomUUID().toString();
        try {
            int inserted = jdbc.update("""
                INSERT INTO inspections (
                  id, inspection_master_no, corp_no, source_data_date,
                  inspection_name, facility_type, road_type, road_name, road_number,
                  road_direction, province, city, city_code, address,
                  start_latitude, start_longitude, end_latitude, end_longitude,
                  inspection_length, inspection_width, sidewalk_width, inspection_height, lane_count,
                  divided_traffic, superstructure_type, completion_year,
                  last_inspection_date, last_inspection_result, last_inspection_type,
                  seismic_design_applied, seismic_performance_secured, repair_history,
                  repair_cost, design_live_load, allowable_traffic_load,
                  underpass_height_limit, management_agency, management_phone,
                  manager_no, created_by
                )
                SELECT ?, bm.no, ?, bm.source_data_date,
                       bm.name, bm.facility_type, bm.road_type, bm.road_name, bm.road_number,
                       bm.road_direction, bm.province, bm.city, bm.city_code, bm.address,
                       bm.start_latitude, bm.start_longitude, bm.end_latitude, bm.end_longitude,
                       bm.inspection_length, bm.inspection_width, bm.sidewalk_width, bm.inspection_height, bm.lane_count,
                       bm.divided_traffic, bm.superstructure_type, bm.completion_year,
                       bm.last_inspection_date, bm.last_inspection_result, bm.last_inspection_type,
                       bm.seismic_design_applied, bm.seismic_performance_secured, bm.repair_history,
                       bm.repair_cost, bm.design_live_load, bm.allowable_traffic_load,
                       bm.underpass_height_limit, bm.management_agency, bm.management_phone,
                       ?, ?
                  FROM inspection_master bm WHERE bm.no = ?
                """, id, actor.corpNo(), actor.no(), actor.no(), masterNo);
            if (inserted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "원본 교량을 찾을 수 없습니다.");
            Long no = jdbc.queryForObject("SELECT no FROM inspections WHERE id=? AND corp_no=?", Long.class, id, actor.corpNo());
            if (no == null) throw new IllegalStateException("등록된 교량의 번호를 확인할 수 없습니다.");
            return no;
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "이미 우리 기업에 등록된 교량입니다.", ex);
        }
    }

    private void ensureOwnership(long no, CurrentUser actor) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inspections WHERE no=? AND corp_no=?", Integer.class, no, actor.corpNo());
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "교량을 찾을 수 없습니다.");
    }

    /** 현재 담당자 변경과 이력 저장은 같은 트랜잭션으로 실행한다. */
    @Transactional
    public void assignManager(long inspectionNo, long managerNo, Authentication auth) {
        requireManager(auth);
        CurrentUser actor = actor(auth);
        if (inspectionNo <= 0 || managerNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "교량과 담당자를 선택해 주세요.");
        }

        // 같은 교량의 동시 변경을 직렬화하여 이전 담당자 값과 이력을 일치시킨다.
        Long previousManagerNo;
        try {
            previousManagerNo = jdbc.queryForObject("""
                SELECT manager_no FROM inspections
                 WHERE no = ? AND corp_no = ?
                 FOR UPDATE
                """, Long.class, inspectionNo, actor.corpNo());
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "교량을 찾을 수 없습니다.");
        }

        Integer eligible = jdbc.queryForObject("""
            SELECT COUNT(*) FROM users
             WHERE no = ? AND corp_no = ? AND status = 'ACTIVE'
            """, Integer.class, managerNo, actor.corpNo());
        if (eligible == null || eligible == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 기업의 활성 작업자를 선택해 주세요.");
        }

        // 동일 담당자를 재선택한 경우에는 수정일시와 이력을 변경하지 않는다.
        if (Objects.equals(previousManagerNo, managerNo)) return;

        int updated = jdbc.update("""
            UPDATE inspections SET manager_no = ?, updated_by = ?
             WHERE no = ? AND corp_no = ?
            """, managerNo, actor.no(), inspectionNo, actor.corpNo());
        if (updated != 1) throw new IllegalStateException("교량 담당자 변경에 실패했습니다.");

        // 이 INSERT가 실패하면 위 UPDATE도 롤백된다.
        jdbc.update("""
            INSERT INTO manager_history
                (inspection_no, corp_no, previous_manager_no, new_manager_no, changed_by)
            VALUES (?, ?, ?, ?, ?)
            """, inspectionNo, actor.corpNo(), previousManagerNo, managerNo, actor.no());
    }

    /** 로그인 기업에 속한 교량의 최근 담당자 변경 이력만 조회한다. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> managerHistory(long inspectionNo, Authentication auth) {
        CurrentUser actor = actor(auth);
        ensureOwnership(inspectionNo, actor);
        return jdbc.query("""
            SELECT h.no, h.previous_manager_no, h.new_manager_no,
                   prev.manager_name AS previous_manager_name,
                   next_user.manager_name AS new_manager_name,
                   changer.manager_name AS changed_by_name,
                   h.changed_at
              FROM manager_history h
              JOIN inspections b ON b.no = h.inspection_no AND b.corp_no = h.corp_no
              LEFT JOIN users prev ON prev.no = h.previous_manager_no
              JOIN users next_user ON next_user.no = h.new_manager_no
              JOIN users changer ON changer.no = h.changed_by
             WHERE h.inspection_no = ? AND h.corp_no = ?
             ORDER BY h.changed_at DESC, h.no DESC
             LIMIT 30
            """, (rs, index) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("no", rs.getLong("no"));
            row.put("previousManagerName", rs.getString("previous_manager_name") == null
                    ? "미배정" : rs.getString("previous_manager_name"));
            row.put("newManagerName", rs.getString("new_manager_name"));
            row.put("changedByName", rs.getString("changed_by_name"));
            Timestamp changedAt = rs.getTimestamp("changed_at");
            row.put("changedAt", changedAt == null ? "" : changedAt.toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            return row;
        }, inspectionNo, actor.corpNo());
    }

    /** FK/미디어가 남아있으면 삭제하지 않는다. 별도 업무 기록을 임의로 지우지 않는다. */
    @Transactional
    public void delete(long inspectionNo, Authentication auth) {
        requireManager(auth);
        CurrentUser actor = actor(auth);
        ensureOwnership(inspectionNo, actor);
        // 팀 최신 DDL: manager_history -> inspections FK가 ON DELETE RESTRICT 이므로
        // 변경 이력이 있는 교량은 삭제하지 않고 명확한 409 오류를 반환한다.
        Integer historyCount = jdbc.queryForObject("""
            SELECT COUNT(*) FROM manager_history
             WHERE inspection_no=? AND corp_no=?
            """, Integer.class, inspectionNo, actor.corpNo());
        if (historyCount != null && historyCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "담당자 변경 이력이 있는 교량은 삭제할 수 없습니다.");
        }

        Integer records = jdbc.queryForObject("""
            SELECT (SELECT COUNT(*) FROM inspections WHERE inspection_no=? AND corp_no=?)
                 + (SELECT COUNT(*) FROM maintenance_records WHERE inspection_no=? AND corp_no=?)
                 + (SELECT COUNT(*) FROM media WHERE p_type='INSPECTION' AND p_id=? AND corp_no=?)
            """, Integer.class, inspectionNo, actor.corpNo(), inspectionNo, actor.corpNo(), inspectionNo, actor.corpNo());
        if (records != null && records > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "연결된 점검·유지보수 기록 또는 이미지가 있어 삭제할 수 없습니다.");
        }
        try {
            jdbc.update("DELETE FROM inspections WHERE no=? AND corp_no=?", inspectionNo, actor.corpNo());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 업무 데이터에서 참조하는 교량은 삭제할 수 없습니다.", ex);
        }
    }
}
