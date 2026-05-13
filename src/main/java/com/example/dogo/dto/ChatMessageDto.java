package com.example.dogo.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageDto {
    private Long roomId;
    private Long senderNo;
    private String senderNickname;
    private String senderProfileImage;
    private String content;
    private String type; // ENTER, TALK
    private LocalDateTime createdAt;
}
