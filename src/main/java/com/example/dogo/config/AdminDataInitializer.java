package com.example.dogo.config;

import com.example.dogo.entity.user.User;
import com.example.dogo.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.id}")
    private String adminId;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 이미 해당 아이디의 계정이 존재하는지 확인
        if (userRepository.findByLoginId(adminId).isPresent()) {
            log.info("관리자 계정('{}')이 이미 존재합니다. 자동 생성을 건너뜁니다.", adminId);
            return;
        }

        log.info("관리자 계정이 존재하지 않습니다. 자동 생성을 시작합니다: {}", adminId);

        // 관리자 계정 생성
        User admin = new User(
                adminId,
                passwordEncoder.encode(adminPassword),
                "admin@dogo.com",
                "최고관리자",
                "010-0000-0000",
                null
        );
        
        // 권한을 ADMIN으로 설정
        admin.setRole("ADMIN");

        userRepository.save(admin);
        
        log.info("관리자 계정 생성이 완료되었습니다.");
        log.info("ID: {}, PW: (설정된 비밀번호)", adminId);
    }
}
