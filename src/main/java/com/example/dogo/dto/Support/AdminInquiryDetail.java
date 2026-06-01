package com.example.dogo.dto.Support;

import java.util.List;

public record AdminInquiryDetail(
        Long id,
        String category,
        String categoryLabel,
        String title,
        String content,
        Long userId,
        String userNickname,
        String userEmail,
        String status,
        String answer,
        String createdAt,
        String answeredAt,
        List<InquiryFileView> files
) {
}
