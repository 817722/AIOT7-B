package com.aiotb.project.service.inspections;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.aiotb.project.domain.media.Media; // 👈 Media 엔티티/enum 임포트 추가
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
                        Media.ParentType.INSPECTION, // 👈 Media.ParentType 으로 수정
                        inspectionNo,
                        actor.userNo()
                );
            }
        }
    }

    public List<MediaInfo> getVideos(Long inspectionNo, Actor actor) {
        return mediaService.getMediaList(Media.ParentType.INSPECTION, inspectionNo, actor); // 👈 Media.ParentType 으로 수정
    }
}