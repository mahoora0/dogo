package com.example.dogo.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SmsService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    // In-memory storage for verification codes: <phone, VerificationInfo>
    private final ConcurrentHashMap<String, VerificationInfo> verificationCodes = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken)) {
            log.info("Twilio credentials are not configured. SMS verification will run in development logging mode.");
            return;
        }
        Twilio.init(accountSid, authToken);
    }

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

    public void sendVerificationCode(String toPhoneNumber) {
        String code = generateRandomCode();
        
        // Save code for 30 minutes
        verificationCodes.put(toPhoneNumber, new VerificationInfo(code, TimeUnit.MINUTES.toMillis(30)));

        // [DEVELOPMENT MODE] Log request without exposing the verification code.
        log.info("[SMS Verification] code generated for phone suffix={}", phoneSuffix(toPhoneNumber));

        /* Twilio sending logic commented out for development
        String formattedNumber = formatToE164(toPhoneNumber);
        String formattedFrom = fromPhoneNumber.replaceAll("\\s", "");

        try {
            Message.creator(
                    new PhoneNumber(formattedNumber),
                    new PhoneNumber(formattedFrom),
                    "[두고내림] 인증번호는 [" + code + "] 입니다."
            ).create();
        } catch (Exception e) {
            System.err.println("Twilio SMS send failed: " + e.getMessage());
            throw e;
        }
        */
    }

    public boolean verifyCode(String phoneNumber, String code) {
        VerificationInfo info = verificationCodes.get(phoneNumber);
        if (info != null && !info.isExpired() && info.code.equals(code)) {
            verificationCodes.remove(phoneNumber);
            return true;
        }
        return false;
    }

    private String generateRandomCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    private String phoneSuffix(String phone) {
        String clean = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (clean.length() <= 4) {
            return clean;
        }
        return clean.substring(clean.length() - 4);
    }

    private String formatToE164(String phone) {
        String clean = phone.replaceAll("[^0-9]", "");
        if (clean.startsWith("0") && !clean.startsWith("00")) {
            return "+82" + clean; // 010... -> +82010...
        }
        if (!clean.startsWith("+")) {
            return "+" + clean;
        }
        return clean;
    }
}
