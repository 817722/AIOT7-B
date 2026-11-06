package com.aiotb.project.service.media; // 본인 패키지 경로 확인

import com.aiotb.project.domain.media.Media;
import com.aiotb.project.repository.media.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/";

    @Transactional
    public void saveFile(MultipartFile file, Long corpNo, Media.ParentType parentType, Long parentId, Long uploadedBy) {
        if (file == null || file.isEmpty()) return;

        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String savedFileName = UUID.randomUUID().toString() + "_" + originalFilename;
            File dest = new File(uploadDir + savedFileName);
            file.transferTo(dest);

            Media media = new Media(
                    corpNo,
                    parentType,
                    parentId,
                    Media.MediaType.VIDEO,
                    false,
                    uploadDir + savedFileName,
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    uploadedBy
            );

            mediaRepository.save(media);
        } catch (IOException e) {
            throw new RuntimeException("드론 영상 파일 저장 실패: " + e.getMessage());
        }
    }

    // 👇 [추가] 2. 특정 대상의 미디어 목록 조회 메서드
    public List<Media> getMediaList(Media.ParentType parentType, Long parentId) {
        return mediaRepository.findByParentTypeAndParentId(parentType, parentId);
    }

    // 👇 [추가] 3. 미디어 단건 조회 메서드 (삭제 시 파일 경로 찾기용)
    public Media getMediaById(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("해당 미디어를 찾을 수 없습니다. ID: " + mediaId));
    }

    // 👇 [추가] 4. 미디어 DB 데이터 삭제 메서드
    @Transactional
    public void deleteMedia(Long mediaId) {
        mediaRepository.deleteById(mediaId);
    }


}