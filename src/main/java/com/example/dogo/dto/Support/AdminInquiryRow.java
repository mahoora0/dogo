package com.example.dogo.dto.Support;

public record AdminInquiryRow(
        Long id,
        String title,
        Long userId,
        String userNickname,
        String userEmail,
        String status
) {
}
