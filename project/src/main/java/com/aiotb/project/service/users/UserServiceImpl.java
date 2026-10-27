package com.aiotb.project.service.users;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.dto.users.UserJoinDto;
import com.aiotb.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Override
    public User join(UserJoinDto dto) {
        // 아이디 중복 확인
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .managerName(dto.getManagerName())
                .phone(dto.getPhone())
                .enabled(true)
                .build();
        // 기본 사용자 권한 추가
        user.addAuth("ROLE_USER");
        // 회원 정보 등록
        return userRepository.save(user);

    }

    @Override
    public User select(String username) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return user;

    }

}
