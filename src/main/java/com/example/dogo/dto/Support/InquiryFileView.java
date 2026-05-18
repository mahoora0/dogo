package com.example.dogo.dto.Support;

public record InquiryFileView(
        Long fileId,
        String originalName,
        String fileUrl,
        Long fileSize
) {
}
