package com.example.dogo.dto.Support;

import java.util.List;

public record InquiryDetail(
        Long id,
        String categoryLabel,
        String title,
        String content,
        String status,
        String answer,
        String createdAt,
        String answeredAt,
        List<InquiryFileView> files,
        String writerEmail
) {
}
