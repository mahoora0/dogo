package com.example.dogo.controller;

import com.example.dogo.repository.UserRepository;
import com.example.dogo.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sms")
public class SmsController {

    private final SmsService smsService;
    private final UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");

        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body("연락처를 입력해주세요.");
        }

        // Check if phone already exists
        if (userRepository.existsByPhone(phone)) {
            return ResponseEntity.badRequest().body("이미 등록된 연락처입니다.");
        }

        try {
            smsService.sendVerificationCode(phone);
            return ResponseEntity.ok("인증 번호가 발송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("인증 문자 발송에 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");

        if (smsService.verifyCode(phone, code)) {
            return ResponseEntity.ok("인증에 성공했습니다.");
        } else {
            return ResponseEntity.badRequest().body("인증 번호가 일치하지 않거나 만료되었습니다.");
        }
    }
}
