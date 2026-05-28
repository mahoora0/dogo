package com.example.dogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatFileDto {
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
