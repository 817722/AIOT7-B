package com.aiotb.project.service.bridges;

import com.aiotb.project.domain.BridgeInspectionEntity;
import com.aiotb.project.dto.bridges.BridgeInspectionRequestDto; // 👈 올바른 패키지 경로로 깔끔하게 임포트!
import com.aiotb.project.repository.BridgeInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor 
public class BridgeInspectionService {

    private final BridgeInspectionRepository inspectionRepository; // DB 저장소 연결
    private final String uploadDir = "C:/bridge-uploads/";         // 파일이 저장될 내 컴퓨터 폴더 경로

    @Transactional
    public void processInspection(BridgeInspectionRequestDto requestDto) { 
        
        // 화면에서 넘어온 텍스트 데이터를 DB에 저장하는 과정
        BridgeInspectionEntity entity = new BridgeInspectionEntity();
        entity.setKeyword(requestDto.getKeyword());
        entity.setInspectionType(requestDto.getInspectionType());
        entity.setInspector(requestDto.getInspector());
        entity.setStatus(requestDto.getStatus());
        entity.setSafetyMemo(requestDto.getSafetyMemo());
        entity.setRisk(requestDto.getRisk());

        // 진짜로 DB에 INSERT 실행!
        inspectionRepository.save(entity);
        System.out.println(">>> 성공: 텍스트 데이터가 데이터베이스에 저장되었습니다!");

        // 드론 영상 및 파일들을 폴더에 저장하는 과정
        // 단일 드론 영상 저장
        MultipartFile singleVideo = requestDto.getVideoInputSingle();
        if (singleVideo != null && !singleVideo.isEmpty()) {
            saveFileToFolder(singleVideo);
        }

        // 다중 파일 추가 저장
        if (requestDto.getVideoInputMultiple() != null) {
            for (MultipartFile file : requestDto.getVideoInputMultiple()) {
                if (!file.isEmpty()) {
                    saveFileToFolder(file);
                }
            }
        }
    }

    // 파일을 컴퓨터 폴더에 저장해 주는 하부 메서드
    private void saveFileToFolder(MultipartFile file) {
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();  // 폴더가 없으면 자동 생성
            }

            // 파일 이름이 겹치면 안 되므로 앞에 랜덤 문자열(UUID)을 붙여줍니다.
            String originalFilename = file.getOriginalFilename();
            String savedFileName = UUID.randomUUID().toString() + "_" + originalFilename;
            
            // 지정한 경로에 파일 진짜로 쓰기
            File target = new File(uploadDir + savedFileName);
            file.transferTo(target);
            
            System.out.println(">>> 성공: 파일 저장 완료 -> " + savedFileName);
            
        } catch (IOException e) {
            throw new RuntimeException("파일을 저장하는 중에 에러가 났어요!", e);
        }
    }
}