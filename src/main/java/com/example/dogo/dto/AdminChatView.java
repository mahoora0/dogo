package com.example.dogo.dto;

import java.util.List;

public record AdminChatView(
        Long roomId,
        Long ownerNo,
        String ownerNickname,
        String ownerLoginId,
        Long inquirerNo,
        String inquirerNickname,
        String inquirerLoginId,
        List<ChatMessageDto> messages
) {
}
