package com.example.dogo.controller.api;

import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.match.ItemMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchApiController {

    private final ItemMatchService itemMatchService;

    @PostMapping("/mark-as-read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            itemMatchService.markAllAsRead(userDetails.getUser());
        }
        return ResponseEntity.ok().build();
    }
}
