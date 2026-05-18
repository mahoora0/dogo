package com.example.dogo.service;

import com.example.dogo.entity.user.User;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + loginId));

        // 계정 상태 직접 검증 (이용 정지 BANNED 또는 탈퇴 WITHDRAWN 계정 로그인 원천 차단)
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new org.springframework.security.authentication.DisabledException(
                "BANNED".equals(user.getStatus()) 
                    ? "이용 정지된 계정입니다. 고객센터에 문의해 주세요." 
                    : "탈퇴 유예 처리된 계정입니다."
            );
        }

        return new CustomUserDetails(user);
    }
}
