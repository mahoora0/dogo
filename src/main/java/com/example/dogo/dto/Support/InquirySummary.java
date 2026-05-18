package com.example.dogo.dto.Support;

public record InquirySummary(
        Long id,
        String category,
        String categoryLabel,
        String title,
        String content,
        String createdAt,
        String status,
        boolean isSecret
) {
}
