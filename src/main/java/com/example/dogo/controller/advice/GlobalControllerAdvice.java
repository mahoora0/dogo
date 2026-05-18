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
            boolean hasLostItems = lostItemService.hasLostItems(userDetails.getUser());
            model.addAttribute("hasLostItems", hasLostItems);
            
            // 2. 분실물이 등록된 경우에만 실시간 매칭 알림 수 조회
            if (hasLostItems) {
                // [핵심] 매칭을 먼저 실행해야 바로 아래 getUnreadMatchCount()가 최신 값을 반환함
                // (@ModelAttribute는 컨트롤러보다 먼저 실행되므로 여기서 매칭을 돌려야 함)
                itemMatchService.matchForUserLostItems(userDetails.getUser());

                long unreadMatchCount = itemMatchService.getUnreadMatchCount(userDetails.getUser());
                List<MatchCandidateView> topMatches = itemMatchService.getTopMatchesForNotification(userDetails.getUser());
                
                model.addAttribute("unreadMatchCount", unreadMatchCount);
                model.addAttribute("topMatches", topMatches);
            } else {
                model.addAttribute("unreadMatchCount", 0L);
                model.addAttribute("topMatches", List.of());
            }

            // 3. [TODO] 채팅 읽지 않은 알림 수
            //    채팅방 구현 완료 후 여기서 실제 DB 조회로 교체 예정
            //    현재는 0으로 고정 (채팅 배지 비표시)
            // String requestURI = request.getRequestURI();
            // int unreadChatCount = "/chat".equals(requestURI) ? 0 : chatService.getUnreadCount(user);
            model.addAttribute("unreadChatCount", 0);
        }
    }
}
