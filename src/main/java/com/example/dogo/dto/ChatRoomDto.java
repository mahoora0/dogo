package com.example.dogo.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomDto {
    private Long roomId;
    private String itemTitle;
    private String itemThumbnail;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String otherParticipantNickname;
    private String otherParticipantProfileImage;
    private Long otherParticipantNo;
    private Integer unreadCount;
    private String itemType; // FOUND, LOST
    private Long itemId;
}
