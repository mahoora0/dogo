package com.example.dogo.controller.advice;

import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.item.LostItemService;
import com.example.dogo.service.match.ItemMatchService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final ItemMatchService itemMatchService;
    private final LostItemService lostItemService;

    @ModelAttribute
    public void addAttributes(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            // 1. 회원이 최소 1개 이상의 분실물을 등록했는지 확인
            //    → 분실물이 없는 회원에게는 매칭 알림 자체를 노출하지 않음
            boolean hasLostItems = lostItemService.hasLostItems(userDetails.getUser());
            model.addAttribute("hasLostItems", hasLostItems);

            if (hasLostItems) {
                // 2. 매칭 알고리즘 실행 (페이지 이동마다 실행되지만 중복 방지 로직이 내부에 있음)
                //    - itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId() 로 이미 저장된 매칭은 재생성하지 않음
                //    - 따라서 배지를 클릭해 READ 처리된 매칭은 같은 습득물로 다시 CANDIDATE가 생성되지 않음
                //    - 진짜 새로운 습득물이 등록됐을 때만 새 CANDIDATE 생성 → 배지 재표시됨
                itemMatchService.matchForUserLostItems(userDetails.getUser());

                // 3. 배지 숫자: CANDIDATE 상태인 미읽음 매칭 건수
                //    드롭다운 내용: 읽음 여부 무관하게 점수 상위 3개 (클릭 후 이동해도 내용 유지됨)
                long unreadMatchCount = itemMatchService.getUnreadMatchCount(userDetails.getUser());
                List<MatchCandidateView> topMatches = itemMatchService.getTopMatchesForNotification(userDetails.getUser());

                model.addAttribute("unreadMatchCount", unreadMatchCount);
                model.addAttribute("topMatches", topMatches);
            } else {
                model.addAttribute("unreadMatchCount", 0L);
                model.addAttribute("topMatches", List.of());
            }

            // 4. [TODO] 채팅 읽지 않은 알림 수
            //    채팅방 구현 완료 후 여기서 실제 DB 조회로 교체 예정
            //    현재는 0으로 고정 (채팅 배지 비표시)
            // int unreadChatCount = chatService.getUnreadCount(userDetails.getUser());
            model.addAttribute("unreadChatCount", 0);
        }
    }
}
