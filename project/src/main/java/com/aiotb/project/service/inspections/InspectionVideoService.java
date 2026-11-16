package com.aiotb.project.service.inspections;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.domain.media.Media;
import com.aiotb.project.service.media.MediaService;
import com.aiotb.project.service.media.MediaService.MediaInfo;
import com.aiotb.project.service.users.CurrentUserService.Actor;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionVideoService {

    private final MediaService mediaService;

    @Transactional
    public void saveInspectionVideos(Long inspectionNo, List<MultipartFile> files, Actor actor) {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                mediaService.saveFile(
                        file,
                        actor.corpNo(),
                        Media.ParentType.INSPECTION, // Media.ParentType 으로 수정
                        inspectionNo,
                        actor.userNo(),
                        Media.MediaType.VIDEO // 드론 영상 업로드에는 영상(MP4·WebM)만 허용
                );
            }
        }
    }

    public List<MediaInfo> getVideos(Long inspectionNo, Actor actor) {
        // 영상 목록에는 영상만 반환 (사진 제외)
        return mediaService.getMediaList(Media.ParentType.INSPECTION, inspectionNo, actor, Media.MediaType.VIDEO);
    }
}