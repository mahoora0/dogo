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
import org.springframework.util.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${admin.id}")
    private String adminId;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 기존 DB 하위 호환성을 위해 WITHDRAWN_AT 컬럼 추가 시도
        try {
            jdbcTemplate.execute("ALTER TABLE USERS ADD COLUMN WITHDRAWN_AT DATETIME NULL");
            log.info("USERS 테이블에 WITHDRAWN_AT 컬럼을 성공적으로 생성하였습니다.");
        } catch (Exception e) {
            log.debug("USERS 테이블에 WITHDRAWN_AT 컬럼이 이미 존재하거나 추가 과정이 건너뛰어졌습니다.");
        }

        // 기존 DB 하위 호환성을 위해 REPORT_COUNT_ADJUSTMENT 컬럼 추가 시도
        try {
            jdbcTemplate.execute("ALTER TABLE USERS ADD COLUMN REPORT_COUNT_ADJUSTMENT INT NOT NULL DEFAULT 0");
            log.info("USERS 테이블에 REPORT_COUNT_ADJUSTMENT 컬럼을 성공적으로 생성하였습니다.");
        } catch (Exception e) {
            log.debug("USERS 테이블에 REPORT_COUNT_ADJUSTMENT 컬럼이 이미 존재하거나 추가 과정이 건너뛰어졌습니다.");
        }

        // USERS 테이블에서 불필요해진 PHONE 컬럼 제거 시도
        try {
            jdbcTemplate.execute("ALTER TABLE USERS DROP COLUMN PHONE");
            log.info("USERS 테이블에서 PHONE 컬럼을 성공적으로 제거하였습니다.");
        } catch (Exception e) {
            log.debug("USERS 테이블에서 PHONE 컬럼이 존재하지 않거나 이미 제거되었습니다.");
        }

        // 더미 유저 생성 호출 (Early return에 막히지 않도록 이곳에서 호출)
        seedDummyUsers();

        if (!StringUtils.hasText(adminPassword)) {
            log.warn("admin.password가 비어 있어 관리자 계정 자동 생성을 건너뜁니다.");
            return;
        }

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
                null
            );
        
        // 권한을 ADMIN으로 설정
        admin.setRole("ADMIN");

        userRepository.save(admin);
        
        log.info("관리자 계정 생성이 완료되었습니다.");
        log.info("ID: {}, PW: (설정된 비밀번호)", adminId);
    }

    private void seedDummyUsers() {
        for (char c = 'a'; c <= 'z'; c++) {
            String id = String.valueOf(c).repeat(4);
            if (userRepository.findByLoginId(id).isPresent()) {
                continue;
            }
            String password = String.valueOf(c).repeat(7) + "!";
            String email = id + "@test.com";
            String nickname = id;
            
            User user = new User(
                id,
                passwordEncoder.encode(password),
                email,
                nickname,
                null
            );
            userRepository.save(user);
        }
        log.info("26명의 더미 유저 생성이 완료되었습니다. (aaaa ~ zzzz)");
    }
}
