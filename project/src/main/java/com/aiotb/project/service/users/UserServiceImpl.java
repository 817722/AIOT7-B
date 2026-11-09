package com.aiotb.project.service.users;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.domain.corps.Corp;
import com.aiotb.project.domain.users.User;
import com.aiotb.project.dto.corps.CorpDto;
import com.aiotb.project.dto.corps.VerifiedBusiness;
import com.aiotb.project.dto.users.UserJoinDto;
import com.aiotb.project.repository.users.UserRepository;
import com.aiotb.project.service.corps.CorpService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final CorpService corpService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Override
    @Transactional
    public User join(UserJoinDto dto, VerifiedBusiness verifiedBusiness) {
        if (verifiedBusiness == null || verifiedBusiness.expired()) {
            throw new IllegalArgumentException("기업 인증 유효시간이 지났습니다. 다시 인증해 주세요.");
        }
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        CorpDto corpDto = new CorpDto();
        corpDto.setBisNo(verifiedBusiness.businessNo());
        corpDto.setCeoName(verifiedBusiness.representativeName());
        corpDto.setCorpName(verifiedBusiness.corpName());
        corpDto.setOpenDate(verifiedBusiness.openDate());

        // 가입 전에 같은 사업자번호의 기업이 이미 있는지 확인한다.
        // - 신규 기업: 최초 가입자 = ADMIN(ACTIVE), 기업은 SUPER_ADMIN 승인 전까지 PENDING
        // - 기존 기업: 가입자 = INSPECTOR(PENDING), 같은 기업 ADMIN이 승인
        boolean existingCorp = corpService.isVerified(verifiedBusiness.businessNo());

        // 가입 마지막 단계에서 국세청 정보를 재검증하고, 동일 트랜잭션 안에서 기업/사용자를 생성한다.
        Corp corp = corpService.verifyAndRegister(corpDto);
        if (existingCorp && (corp.getStatus() == Corp.Status.REJECTED || corp.getStatus() == Corp.Status.SUSPENDED)) {
            throw new IllegalArgumentException("가입이 반려되었거나 이용이 정지된 기업에는 담당자 가입을 신청할 수 없습니다.");
        }

        User user = User.builder()
                .corp(corp)
                .username(dto.getUsername().trim())
                .password(passwordEncoder.encode(dto.getPassword()))
                .managerName(dto.getManagerName())
                .phone(dto.getPhone())
                .status(existingCorp ? User.Status.PENDING : User.Status.ACTIVE)
                .build();
        // SUPER_ADMIN은 화면에서 생성할 수 없으며 SQL로만 등록한다.
        user.addAuth(existingCorp ? "INSPECTOR" : "ADMIN");
        return userRepository.saveAndFlush(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User select(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }
}
