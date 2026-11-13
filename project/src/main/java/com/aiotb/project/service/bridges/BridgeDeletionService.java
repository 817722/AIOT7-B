package com.aiotb.project.service.bridges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * 교량 삭제 전용 서비스.
 * - 점검/유지보수 기록이 있으면 교량 삭제 거부
 * - 교량 이미지의 DB 정보와 담당자 변경 이력을 교량과 같은 트랜잭션에서 정리
 * - 실제 이미지 파일은 DB 커밋 후 삭제
 * - bridge_recent는 DDL-ALOHA(5)의 ON DELETE CASCADE에 따라 자동 정리
 */
@Service
@RequiredArgsConstructor
public class BridgeDeletionService {
    private static final Pattern IMAGE_NAME =
            Pattern.compile("[0-9a-f-]{36}\\.(?:jpg|png|webp)");

    private final JdbcTemplate jdbc;

    @Value("${bridge.images.storage-dir:./uploads/bridge-images}")
    private String imageStorageDir;

    private record BridgeMedia(long no, String type, String path) { }

    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(long bridgeNo, long corpNo) {
        // 같은 교량에 대한 대표 이미지 업로드/교량 삭제가 서로 경합하지 않도록 잠급니다.
        List<Long> bridges = jdbc.query("""
                SELECT no FROM bridges WHERE no = ? AND corp_no = ? FOR UPDATE
                """, (rs, rowNum) -> rs.getLong("no"), bridgeNo, corpNo);
        if (bridges.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "소속 기업의 교량을 찾을 수 없습니다.");
        }

        Integer linkedRecords = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM inspections
                         WHERE bridge_no = ? AND corp_no = ?)
                     + (SELECT COUNT(*) FROM maintenance_records
                         WHERE bridge_no = ? AND corp_no = ?)
                """, Integer.class, bridgeNo, corpNo, bridgeNo, corpNo);
        if (linkedRecords != null && linkedRecords > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "연결된 점검·유지보수 기록이 있어 교량을 삭제할 수 없습니다.");
        }

        List<BridgeMedia> images = jdbc.query("""
                SELECT no, type, path FROM media
                 WHERE p_type = 'BRIDGE' AND p_id = ? AND corp_no = ?
                 FOR UPDATE
                """, (rs, rowNum) -> new BridgeMedia(
                        rs.getLong("no"), rs.getString("type"), rs.getString("path")),
                bridgeNo, corpNo);

        Path root = Path.of(imageStorageDir).toAbsolutePath().normalize();
        List<Path> pathsToRemove = new ArrayList<>();
        for (BridgeMedia media : images) {
            if (!"IMAGE".equals(media.type()) || media.path() == null
                    || !IMAGE_NAME.matcher(media.path()).matches()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "지원하지 않는 교량 미디어 형식 또는 저장 경로가 있어 삭제를 보류했습니다.");
            }
            Path path = root.resolve(media.path()).normalize();
            if (!path.startsWith(root) || Files.isSymbolicLink(path)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "교량 대표 이미지 저장 경로가 안전하지 않아 삭제를 보류했습니다.");
            }
            // 다른 업무 기록이 물리 파일을 공유하는 경우 파일을 지우지 않습니다.
            Integer otherReferences = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM media WHERE path = ? AND no <> ?
                    """, Integer.class, media.path(), media.no());
            if (otherReferences != null && otherReferences > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "다른 기록에서 공유 중인 이미지 파일이 있어 삭제를 보류했습니다.");
            }
            pathsToRemove.add(path);
        }

        try {
            if (!images.isEmpty()) {
                int deletedMedia = jdbc.update("""
                        DELETE FROM media
                         WHERE p_type = 'BRIDGE' AND p_id = ? AND corp_no = ?
                        """, bridgeNo, corpNo);
                if (deletedMedia != images.size()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "이미지 데이터가 변경되어 삭제를 보류했습니다. 다시 시도해 주세요.");
                }
            }
            // DDL(5): manager_history -> bridges 는 ON DELETE RESTRICT.
            // 요구한 삭제 정책을 위해 교량 자체의 담당자 변경 이력도 함께 정리합니다.
            jdbc.update("DELETE FROM manager_history WHERE bridge_no = ? AND corp_no = ?", bridgeNo, corpNo);

            int deletedBridge = jdbc.update(
                    "DELETE FROM bridges WHERE no = ? AND corp_no = ?", bridgeNo, corpNo);
            if (deletedBridge != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "교량을 삭제하지 못했습니다. 다시 시도해 주세요.");
            }
        } catch (DataIntegrityViolationException ex) {
            // 최신 팀원 코드에 새로운 참조 테이블이 추가된 경우에는 안전하게 삭제를 거부합니다.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "다른 업무 기록에서 참조 중인 교량은 삭제할 수 없습니다.", ex);
        }

        if (!pathsToRemove.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    for (Path path : pathsToRemove) {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            org.slf4j.LoggerFactory.getLogger(BridgeDeletionService.class)
                                    .warn("교량 삭제 후 이미지 파일 정리 실패: {}", path, ex);
                        }
                    }
                }
            });
        }
    }
}
