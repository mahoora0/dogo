package com.example.dogo.dto.Support;

public record AdminInquiryDetail(
        Long id,
        String category,
        String categoryLabel,
        String title,
        String content,
        Long userId,
        String status,
        String answer,
        String createdAt,
        String answeredAt
) {
}
