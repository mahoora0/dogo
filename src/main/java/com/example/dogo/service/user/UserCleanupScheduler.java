package com.example.dogo.service.user;

import com.example.dogo.entity.user.User;
import com.example.dogo.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserRepository userRepository;
    private final UserHardDeleteService userHardDeleteService;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupWithdrawnUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        log.info("[WITHDRAWN USER CLEANUP] 7일 전 탈퇴 회원 하드 삭제 스케줄러 기동. 기준 시점: {}", cutoff);

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
                userHardDeleteService.deleteUser(userNo);
                log.info("[WITHDRAWN USER CLEANUP] 회원 ID: {} ({}) 관련 연관 데이터 및 계정이 데이터베이스에서 영구 삭제되었습니다.", userNo, nickname);
            } catch (Exception e) {
                log.error("[WITHDRAWN USER CLEANUP] 회원 ID: {} 데이터 삭제 중 에러 발생: {}", userNo, e.getMessage(), e);
            }
        }

        log.info("[WITHDRAWN USER CLEANUP] 7일 전 탈퇴 회원 하드 삭제 스케줄러 완료.");
    }
}
