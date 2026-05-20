package com.example.dogo.controller.advice;

import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.ChatService;
import com.example.dogo.service.item.LostItemService;
import com.example.dogo.service.match.ItemMatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final ItemMatchService itemMatchService;
    private final LostItemService lostItemService;
    private final ChatService chatService;

    @ModelAttribute
    public void addAttributes(Model model,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return;
        }

        try {

            boolean hasLostItems =
                lostItemService.hasLostItems(userDetails.getUser());

            model.addAttribute("hasLostItems", hasLostItems);

            if (hasLostItems) {

                // 매칭 실행은 여기서 하지 않음
                // itemMatchService.matchForUserLostItems(userDetails.getUser());

                // unread count
                long unreadMatchCount =
                    itemMatchService.getUnreadMatchCount(userDetails.getUser());

                // top matches
                List<MatchCandidateView> topMatches =
                    itemMatchService.getTopMatchesForNotification(userDetails.getUser());

                model.addAttribute("unreadMatchCount", unreadMatchCount);
                model.addAttribute("topMatches", topMatches);

            } else {

                model.addAttribute("unreadMatchCount", 0L);
                model.addAttribute("topMatches", List.of());
            }

            // 채팅 안읽은 개수
            model.addAttribute(
                "unreadChatCount",
                chatService.getUnreadCount(userDetails.getUser())
            );

        } catch (Exception e) {

            log.error("GlobalControllerAdvice error", e);

            // 에러 나도 페이지 자체는 죽지 않게 기본값 세팅
            model.addAttribute("hasLostItems", false);
            model.addAttribute("unreadMatchCount", 0L);
            model.addAttribute("topMatches", List.of());
            model.addAttribute("unreadChatCount", 0);
        }
    }
}