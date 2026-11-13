package com.aiotb.project.service.maintenance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 유지보수 영상 전용 스토리지/DB 어댑터. 팀원의 기존 /api/maintenance와 별도 경로로 사용합니다.
 * 파일은 static 외부에 두고, 모든 요청마다 로그인 기업 및 기록 접근권한을 확인합니다.
 */
@Service
public class MaintenanceVideoService {
    private final JdbcTemplate jdbc;
    private final Path root;
    private final Path mediaRoot;
    private final long maxBytes;

    public MaintenanceVideoService(JdbcTemplate jdbc,
            @Value("${maintenance.videos.storage-dir:./uploads/maintenance-videos}") String directory,
            @Value("${maintenance.videos.max-bytes:209715200}") long maxBytes,
            @Value("${media.storage-dir:./uploads/media}") String mediaDirectory) {
        this.jdbc = jdbc;
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        this.mediaRoot = Paths.get(mediaDirectory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    private record Actor(long no, long corpNo, boolean admin) { }
    private record MaintenanceAccess(long managerNo, long createdBy) { }
    public record VideoFile(Path path, String mimeType) { }

    private Actor actor(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.no, u.corp_no FROM users u
                JOIN corps c ON c.no = u.corp_no AND c.status = 'ACTIVE'
                WHERE u.username = ? AND u.status = 'ACTIVE'
                """, auth.getName());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 확인할 수 없습니다.");
        long no = ((Number) rows.get(0).get("no")).longValue();
        long corp = ((Number) rows.get(0).get("corp_no")).longValue();
        Integer admins = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_auth
                 WHERE user_no = ? AND auth IN ('ADMIN', 'SUPER_ADMIN')
                """, Integer.class, no);
        return new Actor(no, corp, admins != null && admins > 0);
    }

    private MaintenanceAccess requireRecord(Actor actor, long maintenanceNo) {
        return requireRecord(actor, maintenanceNo, false);
    }

    /** 업로드·영상 삭제는 유지보수 기록 삭제와 동일한 순서로 부모 행을 잠급니다. */
    private MaintenanceAccess requireRecordForUpdate(Actor actor, long maintenanceNo) {
        return requireRecord(actor, maintenanceNo, true);
    }

    private MaintenanceAccess requireRecord(Actor actor, long maintenanceNo, boolean forUpdate) {
        String sql = """
                SELECT m.manager_no, m.created_by
                  FROM maintenance_records m
                  JOIN bridges b ON b.no = m.bridge_no AND b.corp_no = m.corp_no
                 WHERE m.no = ? AND m.corp_no = ?
                """ + (forUpdate ? " FOR UPDATE" : "");
        List<Map<String, Object>> rows = jdbc.queryForList(sql, maintenanceNo, actor.corpNo());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "유지보수 기록을 찾을 수 없습니다.");
        Map<String, Object> data = rows.get(0);
        return new MaintenanceAccess(((Number) data.get("manager_no")).longValue(),
                                     ((Number) data.get("created_by")).longValue());
    }

    private void requireEdit(Actor actor, MaintenanceAccess access) {
        if (!actor.admin() && actor.no() != access.managerNo() && actor.no() != access.createdBy()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "기록 작성자·담당자·관리자만 영상을 수정할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Authentication auth, long maintenanceNo) {
        Actor actor = actor(auth);
        requireRecord(actor, maintenanceNo);
        return jdbc.query("""
                SELECT f.no, f.origin_name, f.file_size, f.mime_type
                  FROM media f
                 WHERE f.p_type = 'MAINTENANCE' AND f.type = 'VIDEO'
                   AND f.p_id = ? AND f.corp_no = ?
                 ORDER BY f.created_at DESC, f.no DESC
                """, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("no", rs.getLong("no"));
            row.put("name", rs.getString("origin_name"));
            row.put("size", rs.getLong("file_size"));
            row.put("mimeType", rs.getString("mime_type"));
            return row;
        }, maintenanceNo, actor.corpNo());
    }

    @Transactional
    public long upload(Authentication auth, long maintenanceNo, MultipartFile file) {
        Actor actor = actor(auth);
        requireEdit(actor, requireRecordForUpdate(actor, maintenanceNo));
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "영상 파일을 선택해 주세요.");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "영상 파일 크기 제한을 초과했습니다.");
        }
        String original = safeOriginalName(file.getOriginalFilename());
        String extension = original.toLowerCase(java.util.Locale.ROOT).endsWith(".mp4") ? "mp4"
                : original.toLowerCase(java.util.Locale.ROOT).endsWith(".webm") ? "webm" : "";
        if (extension.isEmpty() || !matchesSignature(file, extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MP4 또는 WebM 형식의 정상적인 영상만 업로드할 수 있습니다.");
        }
        String mime = extension.equals("mp4") ? "video/mp4" : "video/webm";
        String stored = UUID.randomUUID().toString() + "." + extension;
        Path destination = root.resolve(stored);
        try {
            Files.createDirectories(root);
            try (InputStream stream = file.getInputStream()) {
                Files.copy(stream, destination);
            }
        } catch (IOException ex) {
            try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "영상 파일 저장에 실패했습니다.", ex);
        }
        // DB 트랜잭션이 롤백되면 업로드된 파일도 삭제해 고아 파일을 줄입니다.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) deleteQuietly(destination);
            }
        });
        String uuid = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO media (id, corp_no, p_type, p_id, type, is_main, path,
                                   origin_name, mime_type, file_size, uploaded_by)
                VALUES (?, ?, 'MAINTENANCE', ?, 'VIDEO', 0, ?, ?, ?, ?, ?)
                """, uuid, actor.corpNo(), maintenanceNo, stored, original, mime,
                file.getSize(), actor.no());
        Long mediaNo = jdbc.queryForObject("SELECT no FROM media WHERE id = ?", Long.class, uuid);
        if (mediaNo == null) throw new IllegalStateException("저장된 영상 번호를 확인할 수 없습니다.");
        return mediaNo;
    }

    @Transactional(readOnly = true)
    public VideoFile video(Authentication auth, long maintenanceNo, long mediaNo) {
        Actor actor = actor(auth);
        requireRecord(actor, maintenanceNo);
        Map<String, Object> row = requireMedia(actor, maintenanceNo, mediaNo);
        Path file = resolveStored(row.get("path"));
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "영상 파일을 찾을 수 없습니다.");
        }
        String mime = String.valueOf(row.get("mime_type"));
        if (!mime.equals("video/mp4") && !mime.equals("video/webm")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 영상 형식입니다.");
        }
        return new VideoFile(file, mime);
    }

    @Transactional
    public void delete(Authentication auth, long maintenanceNo, long mediaNo) {
        Actor actor = actor(auth);
        requireEdit(actor, requireRecordForUpdate(actor, maintenanceNo));
        Map<String, Object> row = requireMedia(actor, maintenanceNo, mediaNo);
        // 레거시 파일 경로의 경우 파일 삭제는 건너뛰고 메타데이터만 삭제합니다.
        Path file = safeStoredPath(row.get("path"));
        int count = jdbc.update("""
                DELETE FROM media WHERE no = ? AND p_type = 'MAINTENANCE'
                 AND type = 'VIDEO' AND p_id = ? AND corp_no = ?
                """, mediaNo, maintenanceNo, actor.corpNo());
        if (count != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "영상 정보를 찾을 수 없습니다.");
        if (file != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deleteQuietly(file); }
            });
        }
    }

    /** 같은 DB 트랜잭션에서 유지보수와 증거사진·영상 미디어를 함께 삭제합니다.
     * 알 수 없는 저장 경로의 파일은 안전을 위해 삭제를 차단합니다.
     * 호출자는 maintenance_records 행을 FOR UPDATE로 먼저 잠가야 합니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeLinkedMediaForRecord(long corpNo, long maintenanceNo) {
        List<Map<String, Object>> attached = jdbc.queryForList("""
                SELECT no, type, path FROM media
                 WHERE p_type = 'MAINTENANCE' AND p_id = ? AND corp_no = ?
                 FOR UPDATE
                """, maintenanceNo, corpNo);
        List<Path> removeAfterCommit = new java.util.ArrayList<>();
        for (Map<String, Object> item : attached) {
            String kind = String.valueOf(item.get("type"));
            Path safeFile = safeAttachedPath(kind, item.get("path"));
            if (safeFile == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "관리되지 않는 사진 또는 영상 경로가 있어 기록 삭제를 보류했습니다. 저장 경로를 확인해 주세요.");
            }
            Integer shared = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM media
                     WHERE path = ? AND NOT (p_type = 'MAINTENANCE' AND p_id = ? AND corp_no = ?)
                    """, Integer.class, String.valueOf(item.get("path")), maintenanceNo, corpNo);
            if (shared != null && shared > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "다른 기록에서 공유하는 사진 또는 영상 파일이 있어 삭제를 보류했습니다.");
            }
            removeAfterCommit.add(safeFile);
        }
        if (!attached.isEmpty()) {
            int removed = jdbc.update("""
                    DELETE FROM media WHERE p_type = 'MAINTENANCE' AND p_id = ? AND corp_no = ?
                    """, maintenanceNo, corpNo);
            if (removed != attached.size()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "첨부파일 목록이 변경되어 기록 삭제를 보류했습니다. 다시 시도해 주세요.");
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    for (Path file : removeAfterCommit) deleteQuietly(file);
                }
            });
        }
    }

    private Map<String, Object> requireMedia(Actor actor, long maintenanceNo, long mediaNo) {
        List<Map<String, Object>> result = jdbc.queryForList("""
                SELECT path, mime_type FROM media
                 WHERE no = ? AND p_type = 'MAINTENANCE' AND type = 'VIDEO'
                   AND p_id = ? AND corp_no = ?
                """, mediaNo, maintenanceNo, actor.corpNo());
        if (result.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "영상 정보를 찾을 수 없습니다.");
        return result.get(0);
    }

    private Path resolveStored(Object name) {
        Path candidate = safeStoredPath(name);
        if (candidate == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "지원하지 않는 영상 저장 경로입니다.");
        return candidate;
    }

    /** 새 공통 media 폴더와 이전 maintenance-videos 폴더를 함께 인정합니다. */
    private Path safeAttachedPath(String type, Object name) {
        if (name == null) return null;
        String value = String.valueOf(name);
        boolean image = "IMAGE".equals(type);
        boolean video = "VIDEO".equals(type);
        if (!image && !video) return null;
        if (!value.matches(image ? "[0-9a-f-]{36}\\.(jpg|png|webp)"
                                 : "[0-9a-f-]{36}\\.(mp4|webm)")) return null;
        Path media = mediaRoot.resolve(value).normalize();
        if (!media.startsWith(mediaRoot)) return null;
        if (image) return media;
        Path legacy = root.resolve(value).normalize();
        if (!legacy.startsWith(root)) return null;
        // UUID 충돌로 양쪽 저장소에 동일한 이름이 존재하면 어느 쪽인지 불명확하므로 삭제 보류.
        if (Files.isRegularFile(media) && Files.isRegularFile(legacy)) return null;
        return Files.isRegularFile(media) ? media : legacy;
    }

    private Path safeStoredPath(Object name) {
        if (name == null) return null;
        String value = String.valueOf(name);
        if (!value.matches("[0-9a-f-]{36}\\.(mp4|webm)")) return null;
        Path candidate = root.resolve(value).normalize();
        return candidate.startsWith(root) ? candidate : null;
    }

    private static String safeOriginalName(String raw) {
        if (raw == null || raw.isBlank()) return "video.mp4";
        String file = raw.replace('\\', '/');
        file = file.substring(file.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").strip();
        if (file.isBlank()) return "video.mp4";
        return file.length() > 255 ? file.substring(file.length() - 255) : file;
    }

    private static boolean matchesSignature(MultipartFile file, String ext) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (ext.equals("mp4")) {
                return header.length >= 8 && header[4] == 'f' && header[5] == 't'
                        && header[6] == 'y' && header[7] == 'p';
            }
            return header.length >= 4 && (header[0] & 0xff) == 0x1a && (header[1] & 0xff) == 0x45
                    && (header[2] & 0xff) == 0xdf && (header[3] & 0xff) == 0xa3;
        } catch (IOException ex) {
            return false;
        }
    }

    private static void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }
}
