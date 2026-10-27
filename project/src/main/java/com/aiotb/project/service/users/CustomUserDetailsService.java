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

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("계정 정보가 없습니다."));
        if (!user.isEnabled() || user.getCorp() == null || !user.getCorp().isVerified()) {
            throw new UsernameNotFoundException("사용할 수 없는 계정입니다.");
        }
        List<SimpleGrantedAuthority> roles = user.getAuthList().stream()
                .map(auth -> new SimpleGrantedAuthority("ROLE_" + auth.getAuth().name()))
                .toList();
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(roles)
                .accountLocked(false)
                .disabled(false)
                .build();
    }
}
