package com.example.dogo.controller;

import com.example.dogo.repository.user.UserRepository;
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

    @PostMapping("/send-for-find")
    public ResponseEntity<?> sendCodeForFind(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
        }

        // Check if email exists
        if (!userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("가입되지 않은 이메일입니다.");
        }

        try {
            mailService.sendVerificationCodeForFind(email);
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

    @PostMapping("/verify-for-reset")
    public ResponseEntity<?> verifyCodeForPasswordReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String resetToken = mailService.verifyCodeAndIssuePasswordResetToken(email, code);

        if (resetToken == null) {
            return ResponseEntity.badRequest().body("인증 번호가 일치하지 않거나 만료되었습니다.");
        }
        return ResponseEntity.ok(Map.of("resetToken", resetToken));
    }

    @PostMapping("/verify-for-join")
    public ResponseEntity<?> verifyCodeForJoin(@RequestBody Map<String, String> request) {
        return verifyCodeAndIssueToken(request, MailService.VerificationPurpose.JOIN);
    }

    @PostMapping("/verify-for-find-id")
    public ResponseEntity<?> verifyCodeForFindId(@RequestBody Map<String, String> request) {
        return verifyCodeAndIssueToken(request, MailService.VerificationPurpose.FIND_ID);
    }

    private ResponseEntity<?> verifyCodeAndIssueToken(Map<String, String> request, MailService.VerificationPurpose purpose) {
        String token = mailService.verifyCodeAndIssueToken(request.get("email"), request.get("code"), purpose);
        if (token == null) {
            return ResponseEntity.badRequest().body("인증 번호가 일치하지 않거나 만료되었습니다.");
        }
        return ResponseEntity.ok(Map.of("verificationToken", token));
    }
}
