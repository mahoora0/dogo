package com.example.dogo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SmsServiceTest {

    @Test
    void initAllowsMissingTwilioCredentialsInDevelopmentMode() {
        SmsService smsService = new SmsService();
        ReflectionTestUtils.setField(smsService, "accountSid", "");
        ReflectionTestUtils.setField(smsService, "authToken", "");

        assertDoesNotThrow(smsService::init);
    }
}
