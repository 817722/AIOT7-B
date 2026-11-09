package com.aiotb.project.service.inspections;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.domain.bridges.Bridge;
import com.aiotb.project.domain.inspections.Inspection;
import com.aiotb.project.domain.media.Media;
import com.aiotb.project.dto.inspections.InspectionDto;
import com.aiotb.project.repository.bridges.BridgeRepository;
import com.aiotb.project.repository.inspections.InspectionRepository;
import com.aiotb.project.repository.users.UserRepository;
import com.aiotb.project.service.media.MediaService;
import com.aiotb.project.service.users.CurrentUserService.Actor;

import lombok.RequiredArgsConstructor;

/**
 * 점검 기록 REST(/api/inspections) 서비스.
 * 모든 메서드는 Controller가 전달한 로그인 사용자(Actor)의 기업 번호로만 데이터를 다룬다.
 * 요청 DTO의 corpNo / createdBy 값은 사용하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionService {

        private static final List<String> TYPES = List.of("자체 안전점검", "정기 안전점검", "정밀 안전진단", "긴급 안전점검");
        private static final List<String> STATUSES = List.of("진행 대기", "진행 중", "완료");

        private final InspectionRepository inspectionRepository;
        private final BridgeRepository bridgeRepository;
        private final UserRepository userRepository;
        // 미디어 전용 서비스를 주입받습니다.
        private final MediaService mediaService;

        // 1. 점검 기록 등록 (+ 드론 영상/현장 사진 파일 선택 첨부)
        @Transactional
        public String createInspection(InspectionDto.Request request, MultipartFile file, Actor actor) {
                validate(request);

                // 같은 기업의 교량인지 확인 (다른 기업 교량은 존재 여부도 노출하지 않음)
                Bridge bridge = bridgeRepository.findById(request.getBridgeNo())
                        .filter(b -> Objects.equals(b.getCorpNo(), actor.corpNo()))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "교량을 찾을 수 없습니다."));
                requireSameCorpActiveUser(request.getInspectorNo(), actor);

                Inspection inspection = Inspection.builder()
                        .bridge(bridge)
                        .corpNo(actor.corpNo())          // 클라이언트 값 대신 로그인 사용자 기업
                        .title(request.getTitle().strip())
                        .inspectionType(request.getInspectionType())
                        .status(request.getStatus())
                        .inspectionDate(request.getInspectionDate())
                        .inspectorNo(request.getInspectorNo())
                        .inspectionMemo(request.getMemo())
                        .overallResult(request.getOverallResult())
                        .reviewOpinion(request.getReviewOpinion())
                        .createdBy(actor.userNo())       // 클라이언트 값 대신 로그인 사용자
                        .build();

                Inspection savedInspection = inspectionRepository.save(inspection);

                if (file != null && !file.isEmpty()) {
                        // media.p_id 는 inspections.no(숫자 PK)를 가리킨다. (기존 코드는 UUID를 Long으로 변환하다 실패)
                        mediaService.saveFile(file, actor.corpNo(), Media.ParentType.INSPECTION,
                                savedInspection.getNo(), actor.userNo());
                }
                return savedInspection.getId();
        }

        // 2. 특정 교량의 점검 목록 조회 (소속 기업 데이터만)
        public List<InspectionDto.Response> getInspectionsByBridge(Long bridgeNo, Actor actor) {
                return inspectionRepository.findByBridgeNoAndCorpNoOrderByInspectionDateDesc(bridgeNo, actor.corpNo())
                                .stream()
                                .map(InspectionDto.Response::new)
                                .collect(Collectors.toList());
        }

        // 3. 점검 상세 조회
        public InspectionDto.Response getInspectionById(String id, Actor actor) {
                return new InspectionDto.Response(find(id, actor));
        }

        // 4. 점검 기록 수정 (같은 기업 사용자 모두 가능, 점검 담당자 변경은 하지 않음)
        @Transactional
        public void updateInspection(String id, InspectionDto.Request request, Actor actor) {
                validate(request);
                Inspection inspection = find(id, actor);
                if (!Objects.equals(inspection.getBridge().getNo(), request.getBridgeNo())) {
                        throw new IllegalArgumentException("점검 대상 교량은 변경할 수 없습니다.");
                }
                inspection.updateInspection(
                                request.getTitle().strip(),
                                request.getInspectionType(),
                                request.getStatus(),
                                request.getInspectionDate(),
                                request.getMemo(),
                                request.getOverallResult(),
                                request.getReviewOpinion(),
                                actor.userNo());
        }

        // 5. 점검 기록 삭제 (ADMIN 권한은 Controller의 @PreAuthorize 및 SecurityConfig에서 검사)
        @Transactional
        public void deleteInspection(String id, Actor actor) {
                Inspection inspection = find(id, actor);
                // media 는 DB FK가 없으므로, 첨부파일이 남은 기록을 지우면 고아 파일이 생긴다.
                // 연쇄 삭제하지 않고 첨부파일을 먼저 삭제하도록 안내한다.
                if (mediaService.countAttached(Media.ParentType.INSPECTION, inspection.getNo(), actor.corpNo()) > 0) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "첨부파일이 있는 점검 기록은 삭제할 수 없습니다. 첨부파일을 먼저 삭제해 주세요.");
                }
                inspectionRepository.delete(inspection);
        }

        private Inspection find(String id, Actor actor) {
                return inspectionRepository.findByIdAndCorpNo(id, actor.corpNo())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "점검 기록을 찾을 수 없습니다."));
        }

        private void requireSameCorpActiveUser(Long userNo, Actor actor) {
                boolean ok = userNo != null && userRepository.findById(userNo)
                                .filter(u -> Objects.equals(u.getCorp().getNo(), actor.corpNo()))
                                .filter(u -> u.getStatus() == com.aiotb.project.domain.users.User.Status.ACTIVE)
                                .isPresent();
                if (!ok) throw new IllegalArgumentException("같은 기업의 활성 담당자를 선택해 주세요.");
        }

        private static void validate(InspectionDto.Request request) {
                if (request == null || request.getBridgeNo() == null) {
                        throw new IllegalArgumentException("교량 번호는 필수입니다.");
                }
                if (request.getTitle() == null || request.getTitle().isBlank() || request.getTitle().strip().length() > 255) {
                        throw new IllegalArgumentException("점검 기록명을 1~255자로 입력해 주세요.");
                }
                if (!TYPES.contains(request.getInspectionType())) {
                        throw new IllegalArgumentException("지원하지 않는 점검 유형입니다.");
                }
                if (request.getStatus() == null || request.getStatus().isBlank()) {
                        request.setStatus("진행 대기");
                } else if (!STATUSES.contains(request.getStatus())) {
                        throw new IllegalArgumentException("지원하지 않는 진행 상태입니다.");
                }
                if (request.getInspectionDate() == null) {
                        throw new IllegalArgumentException("점검 날짜는 필수입니다.");
                }
        }
}
