package com.example.dogo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    
    // In-memory storage for verification codes: <email, VerificationInfo>
    private final ConcurrentHashMap<String, VerificationInfo> verificationCodes = new ConcurrentHashMap<>();

    private static class VerificationInfo {
        String code;
        long expiryTime;

        VerificationInfo(String code, long durationMillis) {
            this.code = code;
            this.expiryTime = System.currentTimeMillis() + durationMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    public void sendVerificationCode(String toEmail) {
        String code = generateRandomCode();
        
        // Save code for 5 minutes
        verificationCodes.put(toEmail, new VerificationInfo(code, TimeUnit.MINUTES.toMillis(5)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[두고내림] 회원가입 이메일 인증 번호");
        message.setText("인증 번호는 [" + code + "] 입니다. 5분 이내에 입력해주세요.");
        
        mailSender.send(message);
    }

    public void sendVerificationCodeForFind(String toEmail) {
        String code = generateRandomCode();
        
        // Save code for 5 minutes
        verificationCodes.put(toEmail, new VerificationInfo(code, TimeUnit.MINUTES.toMillis(5)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[두고내림] 계정 찾기 이메일 인증 번호");
        message.setText("인증 번호는 [" + code + "] 입니다. 5분 이내에 입력해주세요.");
        
        mailSender.send(message);
    }

    public boolean verifyCode(String email, String code) {
        VerificationInfo info = verificationCodes.get(email);
        if (info != null && !info.isExpired() && info.code.equals(code)) {
            verificationCodes.remove(email); // Success - clear the code
            return true;
        }
        return false;
    }

    private String generateRandomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}
