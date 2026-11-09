package com.aiotb.project.service.users;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiotb.project.domain.users.User;
import com.aiotb.project.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * 승인 상태(users.status, corps.status)는 여기서 거부하지 않고 LoginUser에 담아 둔다.
     * 상태 검사는 비밀번호 확인 후 LoginStatusChecker가 수행한다.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("계정 정보가 없습니다."));
        if (user.getCorp() == null) {
            throw new UsernameNotFoundException("사용할 수 없는 계정입니다.");
        }
        List<SimpleGrantedAuthority> roles = user.getAuthList().stream()
                .map(auth -> new SimpleGrantedAuthority("ROLE_" + auth.getAuth().name()))
                .toList();
        return new LoginUser(user.getUsername(), user.getPassword(), roles,
                user.getNo(), user.getCorp().getNo(), user.getStatus(), user.getCorp().getStatus());
    }
}
