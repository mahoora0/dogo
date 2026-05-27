package com.example.dogo.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SmsService {

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

        // [DEVELOPMENT MODE] Log instead of sending real SMS
        log.info("[SMS Verification] to={}, code={}", toPhoneNumber, code);

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
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
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
