package com.example.dogo.controller.advice;

import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.item.LostItemService;
import com.example.dogo.service.match.ItemMatchService;
import jakarta.servlet.http.HttpServletRequest;
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
    public void addAttributes(Model model, @AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest request) {
        if (userDetails != null) {
            // 1. 회원이 최소 1개 이상의 분실물을 등록했는지 확인
            boolean hasLostItems = lostItemService.hasLostItems(userDetails.getUser());
            model.addAttribute("hasLostItems", hasLostItems);
            
            // 2. 분실물이 등록된 경우에만 실시간 매칭 알림 수 조회
            if (hasLostItems) {
                long unreadMatchCount = itemMatchService.getUnreadMatchCount(userDetails.getUser());
                List<MatchCandidateView> topMatches = itemMatchService.getTopMatchesForNotification(userDetails.getUser());
                
                model.addAttribute("unreadMatchCount", unreadMatchCount);
                model.addAttribute("topMatches", topMatches);
            } else {
                model.addAttribute("unreadMatchCount", 0L);
                model.addAttribute("topMatches", List.of());
            }

            // 3. 채팅 읽지 않은 알림 수 가상 연동 (채팅방 연동 전 임시 데모용 2건 노출, 채팅 페이지 접속 시 0건 처리)
            String requestURI = request.getRequestURI();
            int unreadChatCount = "/chat".equals(requestURI) ? 0 : 2;
            model.addAttribute("unreadChatCount", unreadChatCount);
        }
    }
}
