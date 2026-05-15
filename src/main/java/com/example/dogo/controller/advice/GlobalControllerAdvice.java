package com.example.dogo.controller.advice;

import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.security.CustomUserDetails;
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

    @ModelAttribute
    public void addAttributes(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            long unreadMatchCount = itemMatchService.getUnreadMatchCount(userDetails.getUser());
            List<MatchCandidateView> topMatches = itemMatchService.getTopMatchesForNotification(userDetails.getUser());
            
            model.addAttribute("unreadMatchCount", unreadMatchCount);
            model.addAttribute("topMatches", topMatches);
        }
    }
}
