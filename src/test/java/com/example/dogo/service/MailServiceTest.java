package com.example.dogo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MailServiceTest {

    @Test
    void passwordResetTokenIsIssuedAfterVerificationAndConsumedOnce() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MailService mailService = new MailService(mailSender);
        String email = "user@example.com";

        mailService.sendVerificationCodeForFind(email);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        Matcher codeMatcher = Pattern.compile("\\[(\\d{6})]").matcher(messageCaptor.getValue().getText());
        assertTrue(codeMatcher.find());
        String verificationCode = codeMatcher.group(1);

        String token = mailService.verifyCodeAndIssuePasswordResetToken(email, verificationCode);

        assertNotNull(token);
        assertTrue(mailService.consumePasswordResetToken(token, email));
        assertFalse(mailService.consumePasswordResetToken(token, email));
    }
}
