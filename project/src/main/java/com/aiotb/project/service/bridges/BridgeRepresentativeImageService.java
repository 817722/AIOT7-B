package com.aiotb.project.service.bridges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

/** 기업별 교량 대표 이미지를 media(p_type=BRIDGE)에 저장. 파일은 정적 공개 폴더 밖에 보관. */
@Service
@RequiredArgsConstructor
public class BridgeRepresentativeImageService {
    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final Pattern STORED_NAME = Pattern.compile("[a-f0-9-]{36}\\.(?:jpg|png|webp)");

    private final JdbcTemplate jdbc;
    private final UserRepository users;

    @Value("${bridge.images.storage-dir:./uploads/bridge-images}")
    private String storageDir;

    private record Actor(long no, long corpNo) {}
    public record ImageData(byte[] bytes, String mime) {}
    private record Stored(String filename, String mime) {}

    private Actor actor(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        User user = users.findByUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("등록되지 않은 사용자입니다."));
        if (!user.isEnabled() || user.getCorp() == null || !user.getCorp().isVerified()) {
            throw new AccessDeniedException("사용할 수 없는 기업 계정입니다.");
        }
        return new Actor(user.getNo(), user.getCorp().getNo());
    }

    private long registeredBy(long bridgeNo, Actor actor) {
        List<Long> rows = jdbc.query("SELECT created_by FROM bridges WHERE no=? AND corp_no=?",
                (rs, idx) -> rs.getLong("created_by"), bridgeNo, actor.corpNo());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "교량을 찾을 수 없습니다.");
        return rows.get(0);
    }

    private void checkWrite(Authentication auth, Actor actor, long createdBy) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (!admin && actor.no() != createdBy) {
            throw new AccessDeniedException("교량 등록자 또는 관리자만 대표 이미지를 변경할 수 있습니다.");
        }
    }

    private static String extension(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff &&
                (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "jpg";
        byte[] png = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), png)) return "png";
        if (bytes.length >= 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") &&
                new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) return "webp";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPG, PNG, WebP 이미지 파일만 허용됩니다.");
    }

    private static String mime(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private Path root() { return Path.of(storageDir).toAbsolutePath().normalize(); }
    private static String safeOriginalName(String name) {
        if (name == null || name.isBlank()) return "bridge-image";
        String normalized = name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "");
        if (normalized.isBlank()) return "bridge-image";
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) { /* 로그는 운영 시 별도 모니터링 */ }
    }

    @Transactional
    public void upload(long bridgeNo, MultipartFile file, Authentication auth) {
        Actor actor = actor(auth);
        long createdBy = registeredBy(bridgeNo, actor);
        checkWrite(auth, actor, createdBy);
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 5MB 이하로 첨부해 주세요.");
        }
        final byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드 파일을 읽지 못했습니다.", ex); }
        if (bytes.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "5MB 제한을 초과했습니다.");
        String ext = extension(bytes);
        String name = UUID.randomUUID() + "." + ext;
        Path target = root().resolve(name);
        try {
            Files.createDirectories(root());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지를 저장하지 못했습니다.", ex);
        }

        Path previous = null;
        try {
            List<Map<String, Object>> existing = jdbc.query("""
                SELECT no, path FROM media
                WHERE p_type='BRIDGE' AND p_id=? AND corp_no=? AND is_main=1 FOR UPDATE
                """, (rs, idx) -> Map.of("no", rs.getLong("no"), "path", rs.getString("path")),
                bridgeNo, actor.corpNo());
            if (!existing.isEmpty()) {
                Map<String, Object> old = existing.get(0);
                String oldFilename = String.valueOf(old.get("path"));
                if (STORED_NAME.matcher(oldFilename).matches()) previous = root().resolve(oldFilename);
                jdbc.update("""
                    UPDATE media SET path=?, origin_name=?, mime_type=?, file_size=?, uploaded_by=? WHERE no=?
                    """, name, safeOriginalName(file.getOriginalFilename()), mime(ext), (long) bytes.length,
                        actor.no(), old.get("no"));
            } else {
                jdbc.update("""
                    INSERT INTO media (id, corp_no, p_type, p_id, type, is_main,
                                       path, origin_name, mime_type, file_size, uploaded_by)
                    VALUES (?, ?, 'BRIDGE', ?, 'IMAGE', 1, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), actor.corpNo(), bridgeNo,
                    name, safeOriginalName(file.getOriginalFilename()), mime(ext), (long) bytes.length, actor.no());
            }
            Path oldToDelete = previous;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) deleteQuietly(oldToDelete);
                        else deleteQuietly(target);
                    }
                });
            }
        } catch (RuntimeException ex) {
            deleteQuietly(target);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ImageData read(long bridgeNo, Authentication auth) {
        Actor actor = actor(auth);
        registeredBy(bridgeNo, actor);
        List<Stored> rows = jdbc.query("""
            SELECT path, mime_type FROM media WHERE p_type='BRIDGE' AND p_id=?
                  AND corp_no=? AND is_main=1 LIMIT 1
            """, (rs, idx) -> new Stored(rs.getString("path"), rs.getString("mime_type")),
                bridgeNo, actor.corpNo());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "대표 이미지가 없습니다.");
        Stored stored = rows.get(0);
        if (!STORED_NAME.matcher(stored.filename()).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "저장소에서 관리하지 않는 이미지입니다.");
        }
        try {
            Path img = root().resolve(stored.filename()).normalize();
            if (!img.startsWith(root())) throw new IOException("Invalid file path");
            byte[] content = Files.readAllBytes(img);
            return new ImageData(content, stored.mime());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "대표 이미지 파일을 찾을 수 없습니다.", ex);
        }
    }
}
