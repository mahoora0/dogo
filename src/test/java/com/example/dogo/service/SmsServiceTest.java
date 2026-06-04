package com.example.dogo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SmsServiceTest {

    @Test
    void initAllowsMissingTwilioCredentialsInDevelopmentMode() {
        SmsService smsService = new SmsService();
        ReflectionTestUtils.setField(smsService, "accountSid", "");
        ReflectionTestUtils.setField(smsService, "authToken", "");

        assertDoesNotThrow(smsService::init);
    }

    @Test
    void sendVerificationCodeDoesNotLogVerificationCode(CapturedOutput output) {
        SmsService smsService = new SmsService();

        smsService.sendVerificationCode("010-1234-5678");

        assertThat(output).doesNotContain("code=");
    }
}
