package com.aiotb.project.service.bridges;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
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
 * DDL-ALOHA(1).sql 업무용 교량 CRUD 어댑터. 팀원 기존 /api/bridges REST 코드를 변경하지 않음.
 * bridge_master의 실제 DB PK(no)만 입력받고 교량 속성은 DB의 원본에서 복사한다.
 */
@Service
@RequiredArgsConstructor
public class BridgeUiWriteService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public record CurrentUser(long no, long corpNo) {}

    private CurrentUser actor(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        User user = users.findByUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("사용자가 존재하지 않습니다."));
        if (!user.isEnabled() || user.getCorp() == null || !user.getCorp().isVerified()) {
            throw new AccessDeniedException("인증된 기업 사용자만 이용할 수 있습니다.");
        }
        return new CurrentUser(user.getNo(), user.getCorp().getNo());
    }

    private void requireManager(Authentication auth) {
        boolean allowed = auth != null && auth.getAuthorities().stream().anyMatch(a ->
            "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (!allowed) throw new AccessDeniedException("교량 변경 및 삭제는 관리자 권한이 필요합니다.");
    }

    /** 원본은 DB에 선적재된 bridge_master에서만 검색. 검색어는 바인딩하고 50건으로 제한. */
    @Transactional(readOnly = true)
    public List<Map<String,Object>> catalog(String keyword, Authentication auth) {
        actor(auth);
        if (keyword == null || keyword.isBlank()) return List.of();
        String word = keyword.strip();
        if (word.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어가 너무 깁니다.");
        // SQL LIKE 특수문자를 리터럴로 취급
        String escaped = word.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return jdbc.query("""
            SELECT no, name, facility_type, road_type, road_name, province, city,
                   address, bridge_length, bridge_width, management_agency, source_data_date
              FROM bridge_master
             WHERE name LIKE ? ESCAPE '!'
             ORDER BY name, province, city, no
             LIMIT 50
            """, (rs, idx) -> {
            Map<String,Object> item = new java.util.LinkedHashMap<>();
            long no = rs.getLong("no");
            item.put("catalogId", String.valueOf(no));
            item.put("masterNo", no);
            item.put("name", rs.getString("name"));
            item.put("facilityType", rs.getString("facility_type"));
            item.put("roadType", rs.getString("road_type"));
            item.put("roadName", rs.getString("road_name"));
            item.put("province", rs.getString("province"));
            item.put("city", rs.getString("city"));
            item.put("address", rs.getString("address"));
            item.put("bridgeLength", rs.getBigDecimal("bridge_length"));
            item.put("bridgeWidth", rs.getBigDecimal("bridge_width"));
            item.put("managementAgency", rs.getString("management_agency"));
            java.sql.Date d = rs.getDate("source_data_date");
            item.put("sourceDataDate", d == null ? null : d.toLocalDate());
            return item;
        }, "%" + escaped + "%");
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> managers(Authentication auth) {
        CurrentUser actor = actor(auth);
        return jdbc.query("""
            SELECT no, manager_name
              FROM users WHERE corp_no = ? AND enabled = 1
             ORDER BY manager_name, no
            """, (rs, idx) -> Map.of("no", rs.getLong("no"), "name", rs.getString("manager_name")), actor.corpNo());
    }

    /** 교량 신규 등록: 원본 컬럼은 클라이언트가 아닌 bridge_master에서 복사한다. */
    @Transactional
    public long create(long masterNo, Authentication auth) {
        CurrentUser actor = actor(auth);
        if (masterNo <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "원본 교량을 선택해 주세요.");
        String id = UUID.randomUUID().toString();
        try {
            int inserted = jdbc.update("""
                INSERT INTO bridges (
                  id, bridge_master_no, corp_no, source_data_date,
                  bridge_name, facility_type, road_type, road_name, road_number,
                  road_direction, province, city, city_code, address,
                  start_latitude, start_longitude, end_latitude, end_longitude,
                  bridge_length, bridge_width, sidewalk_width, bridge_height, lane_count,
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
                       bm.bridge_length, bm.bridge_width, bm.sidewalk_width, bm.bridge_height, bm.lane_count,
                       bm.divided_traffic, bm.superstructure_type, bm.completion_year,
                       bm.last_inspection_date, bm.last_inspection_result, bm.last_inspection_type,
                       bm.seismic_design_applied, bm.seismic_performance_secured, bm.repair_history,
                       bm.repair_cost, bm.design_live_load, bm.allowable_traffic_load,
                       bm.underpass_height_limit, bm.management_agency, bm.management_phone,
                       ?, ?
                  FROM bridge_master bm WHERE bm.no = ?
                """, id, actor.corpNo(), actor.no(), actor.no(), masterNo);
            if (inserted == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "원본 교량을 찾을 수 없습니다.");
            Long no = jdbc.queryForObject("SELECT no FROM bridges WHERE id=? AND corp_no=?", Long.class, id, actor.corpNo());
            if (no == null) throw new IllegalStateException("등록된 교량의 번호를 확인할 수 없습니다.");
            return no;
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "해당 교량이 이미 소속 기업에 등록됐거나 DB 제약조건을 충족하지 못했습니다.", ex);
        }
    }

    private void ensureOwnership(long no, CurrentUser actor) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM bridges WHERE no=? AND corp_no=?", Integer.class, no, actor.corpNo());
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "교량을 찾을 수 없습니다.");
    }

    @Transactional
    public void assignManager(long bridgeNo, long managerNo, Authentication auth) {
        requireManager(auth);
        CurrentUser actor = actor(auth);
        ensureOwnership(bridgeNo, actor);
        Integer eligible = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE no=? AND corp_no=? AND enabled=1",
                Integer.class, managerNo, actor.corpNo());
        if (eligible == null || eligible == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 기업의 활성 작업자를 선택해 주세요.");
        jdbc.update("UPDATE bridges SET manager_no=?, updated_by=? WHERE no=? AND corp_no=?",
                managerNo, actor.no(), bridgeNo, actor.corpNo());
    }

    /** FK/미디어가 남아있으면 삭제하지 않는다. 별도 업무 기록을 임의로 지우지 않는다. */
    @Transactional
    public void delete(long bridgeNo, Authentication auth) {
        requireManager(auth);
        CurrentUser actor = actor(auth);
        ensureOwnership(bridgeNo, actor);
        Integer records = jdbc.queryForObject("""
            SELECT (SELECT COUNT(*) FROM inspections WHERE bridge_no=? AND corp_no=?)
                 + (SELECT COUNT(*) FROM maintenance_records WHERE bridge_no=? AND corp_no=?)
                 + (SELECT COUNT(*) FROM media WHERE p_type='BRIDGE' AND p_id=? AND corp_no=?)
            """, Integer.class, bridgeNo, actor.corpNo(), bridgeNo, actor.corpNo(), bridgeNo, actor.corpNo());
        if (records != null && records > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "연결된 점검·유지보수 기록 또는 이미지가 있어 삭제할 수 없습니다.");
        }
        try {
            jdbc.update("DELETE FROM bridges WHERE no=? AND corp_no=?", bridgeNo, actor.corpNo());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "다른 업무 데이터에서 참조하는 교량은 삭제할 수 없습니다.", ex);
        }
    }
}
