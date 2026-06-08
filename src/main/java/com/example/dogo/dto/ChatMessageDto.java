package com.example.dogo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long messageId;
    private Long roomId;
    private Long senderNo;
    private String senderNickname;
    private String senderProfileImage;
    private String content;
    private String type; // ENTER, TALK, FILE
    private LocalDateTime createdAt;
    private String clientMessageId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileGroupId;
    private List<ChatFileDto> files;
}
