package com.example.dogo.service.user;

import com.example.dogo.entity.user.User;
import com.example.dogo.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 매 시간 정각에 실행되어 탈퇴 후 7일이 지난 회원 데이터 및 연관 테이블 데이터를 영구 삭제합니다.
     * (테스트 및 주기적인 모니터링을 위해 매 시간 구동)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupWithdrawnUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        log.info("[WITHDRAWN USER CLEANUP] 7일 전 탈퇴 회원 하드 삭제 스케줄러 기동. 기준 시점: {}", cutoff);

        // 7일 전에 탈퇴한 회원 목록 조회
        List<User> targetUsers = userRepository.findByStatusAndWithdrawnAtBefore("WITHDRAWN", cutoff);

        if (targetUsers.isEmpty()) {
            log.info("[WITHDRAWN USER CLEANUP] 삭제 대상인 7일 경과 탈퇴 회원이 존재하지 않습니다.");
            return;
        }

        log.info("[WITHDRAWN USER CLEANUP] 총 {}명의 7일 경과 탈퇴 회원을 발견했습니다. 정리 프로세스를 시작합니다.", targetUsers.size());

        for (User user : targetUsers) {
            Long userNo = user.getUserNo();
            String nickname = user.getNickname();
            log.info("[WITHDRAWN USER CLEANUP] 회원 ID: {} ({}) 관련 모든 데이터 및 계정 영구 삭제 시작", userNo, nickname);

            try {
                // 1. 소셜 계정 데이터 삭제
                jdbcTemplate.update("DELETE FROM USER_SOCIAL_ACCOUNT WHERE USER_NO = ?", userNo);

                // 2. 채팅 메시지 삭제
                jdbcTemplate.update("DELETE FROM CHAT_MESSAGE WHERE SENDER_NO = ?", userNo);

                // 3. 채팅방 삭제
                jdbcTemplate.update("DELETE FROM CHAT_ROOM WHERE INQUIRER_NO = ? OR OWNER_NO = ?", userNo, userNo);

                // 4. 1:1 문의 파일 및 문의 게시글 삭제
                jdbcTemplate.update("DELETE FROM INQUIRY_FILE WHERE INQUIRY_ID IN (SELECT INQUIRY_ID FROM INQUIRY WHERE USER_NO = ?)", userNo);
                jdbcTemplate.update("DELETE FROM INQUIRY WHERE USER_NO = ?", userNo);

                // 5. 실종동물 신고 매칭, 이미지 임베딩, 이미지, 실종동물 본글 삭제
                jdbcTemplate.update("DELETE FROM ANIMAL_REPORT_MATCH WHERE MISSING_REPORT_ID IN (SELECT REPORT_ID FROM ANIMAL_REPORT WHERE USER_NO = ?) " +
                        "OR SIGHTING_REPORT_ID IN (SELECT REPORT_ID FROM ANIMAL_REPORT WHERE USER_NO = ?)", userNo, userNo);
                jdbcTemplate.update("DELETE FROM ANIMAL_REPORT_IMAGE_EMBEDDING WHERE REPORT_ID IN (SELECT REPORT_ID FROM ANIMAL_REPORT WHERE USER_NO = ?)", userNo);
                jdbcTemplate.update("DELETE FROM ANIMAL_REPORT_IMAGE WHERE REPORT_ID IN (SELECT REPORT_ID FROM ANIMAL_REPORT WHERE USER_NO = ?)", userNo);
                jdbcTemplate.update("DELETE FROM ANIMAL_REPORT WHERE USER_NO = ?", userNo);

                // 6. 실종자 신고 데이터 삭제
                jdbcTemplate.update("DELETE FROM MISSING_PERSON_REPORT WHERE USER_NO = ?", userNo);

                // 7. 분실물 매칭, 이미지, 분실물 본글 삭제
                jdbcTemplate.update("DELETE FROM ITEM_MATCH WHERE LOST_ID IN (SELECT LOST_ID FROM LOST_ITEM WHERE USER_NO = ?)", userNo);
                jdbcTemplate.update("DELETE FROM LOST_ITEM_IMAGE WHERE LOST_ID IN (SELECT LOST_ID FROM LOST_ITEM WHERE USER_NO = ?)", userNo);
                jdbcTemplate.update("DELETE FROM LOST_ITEM WHERE USER_NO = ?", userNo);

                // 8. 습득물 매칭, 이미지, 습득물 본글 삭제
                jdbcTemplate.update("DELETE FROM ITEM_MATCH WHERE FOUND_ID IN (SELECT FOUND_ID FROM FOUND_ITEM WHERE USER_NO = ?)", userNo);
                jdbcTemplate.update("DELETE FROM FOUND_ITEM_IMAGE WHERE FOUND_ID IN (SELECT FOUND_ID FROM FOUND_ITEM WHERE USER_NO = ?)", userNo);
                jdbcTemplate.update("DELETE FROM FOUND_ITEM WHERE USER_NO = ?", userNo);

                // 9. 최종적으로 USERS 테이블에서 회원 영구 삭제
                jdbcTemplate.update("DELETE FROM USERS WHERE USER_NO = ?", userNo);

                log.info("[WITHDRAWN USER CLEANUP] 회원 ID: {} ({}) 관련 연관 데이터 및 계정이 데이터베이스에서 영구 삭제되었습니다.", userNo, nickname);
            } catch (Exception e) {
                log.error("[WITHDRAWN USER CLEANUP] 회원 ID: {} 데이터 삭제 중 에러 발생: {}", userNo, e.getMessage(), e);
            }
        }

        log.info("[WITHDRAWN USER CLEANUP] 7일 전 탈퇴 회원 하드 삭제 스케줄러 완료.");
    }
}
