package com.aiotb.project.service.maintenance;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aiotb.project.domain.maintenance.Maintenance;
import com.aiotb.project.domain.media.Media;
import com.aiotb.project.domain.users.User;
import com.aiotb.project.dto.maintenance.MaintenanceDto;
import com.aiotb.project.repository.bridges.BridgeRepository;
import com.aiotb.project.repository.maintenance.MaintenanceRepository;
import com.aiotb.project.repository.users.UserRepository;
import com.aiotb.project.service.media.MediaService;
import com.aiotb.project.service.users.CurrentUserService.Actor;

import lombok.RequiredArgsConstructor;

// corpNo/userNo는 클라이언트가 아닌 인증 사용자(Actor)에서 가져온다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceServiceImpl implements MaintenanceService {
    private final MaintenanceRepository repository;
    private final BridgeRepository bridgeRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;

    @Override
    @Transactional
    public String create(MaintenanceDto.Request r, Actor actor) {
        validateType(r.getMaintenanceType());
        requireBridge(r.getBridgeNo(), actor);
        requireSameCorpActiveUser(r.getManagerNo(), actor);

        Maintenance m = new Maintenance(
                r.getBridgeNo(),       // bridgeNo (같은 기업 교량 확인 완료)
                actor.corpNo(),        // corpNo  (로그인 사용자 기업)
                r.getTitle().strip(),
                r.getMaintenanceType(),
                r.getMemo(),
                r.getMaintenanceDate(),
                r.getManagerNo(),
                actor.userNo()         // createdBy (로그인 사용자)
        );
        return repository.save(m).getId();
    }

    @Override
    public List<MaintenanceDto.Response> byBridge(Long bridgeNo, Actor actor) {
        return repository.findByBridgeNoAndCorpNoOrderByMaintenanceDateDesc(bridgeNo, actor.corpNo()).stream()
                .map(MaintenanceDto.Response::new).collect(Collectors.toList());
    }

    @Override
    public List<MaintenanceDto.Response> all(Actor actor) {
        // 기존: findAll() → 전체 기업 기록 노출. 소속 기업 기록만 반환하도록 수정.
        return repository.findByCorpNoOrderByMaintenanceDateDesc(actor.corpNo()).stream()
                .map(MaintenanceDto.Response::new).collect(Collectors.toList());
    }

    @Override
    public MaintenanceDto.Response get(String id, Actor actor) {
        return new MaintenanceDto.Response(find(id, actor));
    }

    @Override
    @Transactional
    public void update(String id, MaintenanceDto.Request r, Actor actor) {
        validateType(r.getMaintenanceType());
        Maintenance m = find(id, actor);
        if (!Objects.equals(m.getBridgeNo(), r.getBridgeNo())) {
            throw new IllegalArgumentException("유지보수 대상 교량은 변경할 수 없습니다.");
        }
        // 담당자 변경은 기업 관리자(ADMIN)만 가능
        if (!Objects.equals(m.getManagerNo(), r.getManagerNo())) {
            if (!actor.admin()) throw new AccessDeniedException("담당자 변경은 기업 관리자만 할 수 있습니다.");
            requireSameCorpActiveUser(r.getManagerNo(), actor);
        }
        m.update(r.getTitle().strip(), r.getMaintenanceType(), r.getMemo(), r.getMaintenanceDate(),
                r.getManagerNo(), actor.userNo());
    }

    @Override
    @Transactional
    public void delete(String id, Actor actor) {
        Maintenance m = find(id, actor);
        // media 는 DB FK가 없으므로 첨부파일이 남은 기록은 삭제하지 않는다(고아 파일 방지, 연쇄 삭제 금지).
        if (mediaService.countAttached(Media.ParentType.MAINTENANCE, m.getNo(), actor.corpNo()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "첨부파일이 있는 유지보수 기록은 삭제할 수 없습니다. 첨부파일을 먼저 삭제해 주세요.");
        }
        repository.delete(m);
    }

    private Maintenance find(String id, Actor actor) {
        return repository.findByIdAndCorpNo(id, actor.corpNo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유지보수 기록을 찾을 수 없습니다."));
    }

    private void requireBridge(Long bridgeNo, Actor actor) {
        boolean ok = bridgeNo != null && bridgeRepository.findById(bridgeNo)
                .filter(b -> Objects.equals(b.getCorpNo(), actor.corpNo())).isPresent();
        if (!ok) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "교량을 찾을 수 없습니다.");
    }

    private void requireSameCorpActiveUser(Long userNo, Actor actor) {
        boolean ok = userNo != null && userRepository.findById(userNo)
                .filter(u -> Objects.equals(u.getCorp().getNo(), actor.corpNo()))
                .filter(u -> u.getStatus() == User.Status.ACTIVE)
                .isPresent();
        if (!ok) throw new IllegalArgumentException("같은 기업의 활성 담당자를 선택해 주세요.");
    }

    private void validateType(String type) {
        if (!List.of("균열 보수기록", "단면 복구기록", "구조 보강기록", "하자보수 관리", "기타").contains(type))
            throw new IllegalArgumentException("지원하지 않는 유지보수 분류입니다.");
    }
}
