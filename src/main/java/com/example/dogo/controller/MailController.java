package com.example.dogo.controller;

import com.example.dogo.repository.UserRepository;
import com.example.dogo.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
public class MailController {

    private final MailService mailService;
    private final UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("이미 사용 중인 이메일입니다.");
        }

        try {
            mailService.sendVerificationCode(email);
            return ResponseEntity.ok("인증 번호가 발송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("메일 발송에 실패했습니다.");
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        if (mailService.verifyCode(email, code)) {
            return ResponseEntity.ok("인증에 성공했습니다.");
        } else {
            return ResponseEntity.badRequest().body("인증 번호가 일치하지 않거나 만료되었습니다.");
        }
    }
}
