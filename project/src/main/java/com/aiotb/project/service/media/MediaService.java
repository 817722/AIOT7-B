package com.aiotb.project.service.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.domain.media.Media;
import com.aiotb.project.repository.media.MediaRepository;
import com.aiotb.project.service.users.CurrentUserService.Actor;

import lombok.RequiredArgsConstructor;

/**
 * 점검/유지보수/교량에 연결되는 첨부파일(media) 서비스.
 *
 * [파일과 DB 기록의 분리]
 * - 실제 파일: ${media.storage-dir} (정적 공개 폴더 밖), 파일명은 UUID.확장자
 * - DB(media.path): 저장 파일명만 기록 (서버 절대경로를 저장/노출하지 않음)
 * - 업로드 후 트랜잭션이 롤백되면 파일 삭제, 삭제는 커밋 후 파일 삭제
 *
 * [기업별 접근 제한]
 * - p_id가 가리키는 부모 레코드가 로그인 사용자 기업 소유인지 DB에서 확인한다.
 */
@Service
@RequiredArgsConstructor
public class MediaService {

    private static final Pattern STORED_NAME = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp|mp4|webm)");

    private final MediaRepository mediaRepository;
    private final JdbcTemplate jdbc;

    @Value("${media.storage-dir:./uploads/media}")
    private String storageDir;
    // 기존 기능이 저장한 파일 위치 (조회/삭제 호환용)
    @Value("${maintenance.videos.storage-dir:./uploads/maintenance-videos}")
    private String maintenanceVideoDir;

    // 사진은 개별 파일당 5MB, 한 유지보수 기록당 20장으로 제한합니다.
    @Value("${maintenance.photos.max-bytes:5242880}")
    private long maxPhotoBytes;
    private static final int MAX_MAINTENANCE_PHOTOS = 20;


    /** 화면/API 응답용. 서버 저장 경로는 포함하지 않는다. */
    public record MediaInfo(Long no, String id, String parentType, Long parentId, String type,
                            String originName, String mimeType, Long fileSize, Long uploadedBy,
                            LocalDateTime createdAt) {
        static MediaInfo of(Media m) {
            return new MediaInfo(m.getNo(), m.getId(), m.getParentType().name(), m.getParentId(),
                    m.getType().name(), m.getOriginName(), m.getMimeType(), m.getFileSize(),
                    m.getUploadedBy(), m.getCreatedAt());
        }
    }

    public record StoredFile(Path path, String mimeType, String originName) { }

    private record Detected(String ext, String mime, Media.MediaType type) { }

    /**
     * 파일 저장 + media 레코드 등록.
     * corpNo / uploadedBy 는 반드시 서버에서 확인한 로그인 사용자 값을 전달해야 한다.
     */
    @Transactional
    public Long saveFile(MultipartFile file, Long corpNo, Media.ParentType parentType, Long parentId, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일을 선택해 주세요.");
        }
        if (parentType == null || parentType == Media.ParentType.BRIDGE) {
            // 교량 대표 이미지는 /api/bridge-ui/{no}/image 에서 관리한다.
            throw new IllegalArgumentException("점검(INSPECTION) 또는 유지보수(MAINTENANCE) 기록에만 첨부할 수 있습니다.");
        }
        // 유지보수 첨부는 기록 삭제와 같은 행 잠금 순서로 접근합니다.
        // 공통 /api/media 경로로 요청해도 작성자/담당자/관리자만 업로드할 수 있습니다.
        if (parentType == Media.ParentType.MAINTENANCE) {
            requireMaintenanceEditorForUpdate(parentId, corpNo, uploadedBy);
        } else {
            requireParent(parentType, parentId, corpNo);
        }

        Detected detected = detect(file);
        if (parentType == Media.ParentType.MAINTENANCE
                && detected.type() == Media.MediaType.IMAGE && file.getSize() > maxPhotoBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "이미지는 5MB 이하로 업로드해 주세요.");
        }
        if (parentType == Media.ParentType.MAINTENANCE && detected.type() == Media.MediaType.IMAGE) {
            Integer currentPhotos = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM media WHERE p_type = 'MAINTENANCE'
                      AND p_id = ? AND corp_no = ? AND type = 'IMAGE'
                    """, Integer.class, parentId, corpNo);
            if (currentPhotos != null && currentPhotos >= MAX_MAINTENANCE_PHOTOS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "증거 사진은 최대 20장까지 등록할 수 있습니다.");
            }
        }
        String storedName = UUID.randomUUID() + "." + detected.ext();
        Path root = root();
        Path target = root.resolve(storedName);
        try {
            Files.createDirectories(root);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.", e);
        }
        // DB 저장이 실패(롤백)하면 방금 저장한 파일도 지운다.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) deleteQuietly(target);
                }
            });
        }

        Media media = new Media(corpNo, parentType, parentId, detected.type(), false,
                storedName, safeOriginalName(file.getOriginalFilename()), detected.mime(),
                file.getSize(), uploadedBy);
        return mediaRepository.save(media).getNo();
    }

    /** 유지보수 증거 사진 전용: 영상이 사진 API로 등록되지 않도록 실제 헤더로 검사합니다. */
    @Transactional
    public Long saveMaintenancePhoto(MultipartFile file, Actor actor, Long maintenanceNo) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "증거 사진 파일을 선택해 주세요.");
        }
        if (file.getSize() > maxPhotoBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "사진 1장당 5MB 이하만 허용됩니다.");
        }
        Detected detected = detect(file);
        if (detected.type() != Media.MediaType.IMAGE) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "JPG·PNG·WebP 이미지 파일만 선택해 주세요.");
        }
        return saveFile(file, actor.corpNo(), Media.ParentType.MAINTENANCE, maintenanceNo, actor.userNo());
    }

    /** 삭제 대상이 같은 기업·같은 유지보수의 사진인지 검증한 후 개별 삭제합니다. */
    @Transactional
    public void deleteMaintenancePhoto(Long maintenanceNo, Long photoNo, Actor actor) {
        requireMaintenanceEditorForUpdate(maintenanceNo, actor.corpNo(), actor.userNo());
        Media photo = find(photoNo, actor);
        if (photo.getParentType() != Media.ParentType.MAINTENANCE
                || !maintenanceNo.equals(photo.getParentId())
                || photo.getType() != Media.MediaType.IMAGE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이 유지보수에 등록된 사진을 찾을 수 없습니다.");
        }
        deleteMedia(photoNo, actor);
    }

    /** 부모 레코드를 잠가 전체 기록 삭제와 이미지 업로드/개별 삭제 충돌을 방지합니다. */
    private void requireMaintenanceEditorForUpdate(Long maintenanceNo, Long corpNo, Long userNo) {
        if (maintenanceNo == null || corpNo == null || userNo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유지보수 기록 또는 사용자 정보가 없습니다.");
        }
        var records = jdbc.queryForList("""
                SELECT manager_no, created_by FROM maintenance_records
                 WHERE no = ? AND corp_no = ? FOR UPDATE
                """, maintenanceNo, corpNo);
        if (records.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "소속 기업의 유지보수 기록을 찾을 수 없습니다.");
        }
        long managerNo = ((Number) records.get(0).get("manager_no")).longValue();
        long createdBy = ((Number) records.get(0).get("created_by")).longValue();
        Integer isAdmin = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_auth
                 WHERE user_no = ? AND auth IN ('ADMIN','SUPER_ADMIN')
                """, Integer.class, userNo);
        if (userNo != managerNo && userNo != createdBy && (isAdmin == null || isAdmin == 0)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "유지보수 작성자·담당자·관리자만 사진을 변경할 수 있습니다.");
        }
    }

    /** 특정 기록의 첨부파일 목록 (소속 기업 기록만) */
    @Transactional(readOnly = true)
    public List<MediaInfo> getMediaList(Media.ParentType parentType, Long parentId, Actor actor) {
        requireParent(parentType, parentId, actor.corpNo());
        return mediaRepository.findByParentTypeAndParentIdAndCorpNoOrderByNoDesc(parentType, parentId, actor.corpNo())
                .stream().map(MediaInfo::of).toList();
    }

    /** 첨부파일 내려받기/재생용 파일 위치 (소속 기업 파일만) */
    @Transactional(readOnly = true)
    public StoredFile getFile(Long mediaNo, Actor actor) {
        Media media = find(mediaNo, actor);
        Path path = resolveExisting(media.getPath());
        if (path == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        return new StoredFile(path, media.getMimeType(), media.getOriginName());
    }

    /** 첨부파일 삭제: DB 레코드 삭제 후, 커밋이 성공하면 실제 파일을 삭제한다. */
    @Transactional
    public void deleteMedia(Long mediaNo, Actor actor) {
        Media media = find(mediaNo, actor);
        if (media.isMain()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "교량 대표 이미지는 교량 수정 화면에서 변경해 주세요.");
        }
        Path file = resolveExisting(media.getPath());
        mediaRepository.delete(media);
        if (file != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deleteQuietly(file); }
            });
        }
    }

    /** 기록 삭제 전 첨부파일 존재 여부 확인용 */
    @Transactional(readOnly = true)
    public long countAttached(Media.ParentType parentType, Long parentId, Long corpNo) {
        return mediaRepository.countByParentTypeAndParentIdAndCorpNo(parentType, parentId, corpNo);
    }

    private Media find(Long mediaNo, Actor actor) {
        return mediaRepository.findByNoAndCorpNo(mediaNo, actor.corpNo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));
    }

    /** p_type + p_id 가 가리키는 부모 레코드가 해당 기업 소유인지 확인 (media는 DB FK가 없음) */
    private void requireParent(Media.ParentType parentType, Long parentId, Long corpNo) {
        if (parentType == null || parentId == null || corpNo == null) {
            throw new IllegalArgumentException("첨부 대상 기록을 확인할 수 없습니다.");
        }
        String table = switch (parentType) {
            case BRIDGE -> "bridges";
            case INSPECTION -> "inspections";
            case MAINTENANCE -> "maintenance_records";
        };
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE no = ? AND corp_no = ?", Integer.class, parentId, corpNo);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부 대상 기록을 찾을 수 없습니다.");
        }
    }

    /** 확장자가 아닌 실제 파일 시그니처로 형식을 판별한다. */
    private static Detected detect(MultipartFile file) {
        byte[] h;
        try (InputStream in = file.getInputStream()) {
            h = in.readNBytes(12);
        } catch (IOException e) {
            throw new IllegalArgumentException("업로드 파일을 읽지 못했습니다.");
        }
        if (h.length >= 3 && (h[0] & 0xff) == 0xff && (h[1] & 0xff) == 0xd8 && (h[2] & 0xff) == 0xff)
            return new Detected("jpg", "image/jpeg", Media.MediaType.IMAGE);
        if (h.length >= 8 && (h[0] & 0xff) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G')
            return new Detected("png", "image/png", Media.MediaType.IMAGE);
        if (h.length >= 12 && new String(h, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(h, 8, 4, StandardCharsets.US_ASCII).equals("WEBP"))
            return new Detected("webp", "image/webp", Media.MediaType.IMAGE);
        if (h.length >= 8 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p')
            return new Detected("mp4", "video/mp4", Media.MediaType.VIDEO);
        if (h.length >= 4 && (h[0] & 0xff) == 0x1a && (h[1] & 0xff) == 0x45
                && (h[2] & 0xff) == 0xdf && (h[3] & 0xff) == 0xa3)
            return new Detected("webm", "video/webm", Media.MediaType.VIDEO);
        throw new IllegalArgumentException("JPG·PNG·WebP 이미지 또는 MP4·WebM 영상만 업로드할 수 있습니다.");
    }

    private Path root() {
        return Path.of(storageDir).toAbsolutePath().normalize();
    }

    /** media.path 를 실제 파일로 해석. 관리 중인 저장 폴더 안의 UUID 파일명만 허용한다. */
    private Path resolveExisting(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) return null;
        for (String dir : List.of(storageDir, maintenanceVideoDir)) {
            Path base = Path.of(dir).toAbsolutePath().normalize();
            Path candidate = base.resolve(storedName).normalize();
            if (candidate.startsWith(base) && Files.isRegularFile(candidate)) return candidate;
        }
        return null;
    }

    private static String safeOriginalName(String name) {
        if (name == null || name.isBlank()) return "upload";
        String normalized = name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").strip();
        if (normalized.isBlank()) return "upload";
        return normalized.length() > 255 ? normalized.substring(normalized.length() - 255) : normalized;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }
}
