package com.example.dogo.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesBrowserMessagePayload() throws Exception {
        String payload = """
                {
                  "roomId": 7,
                  "senderNo": 1,
                  "content": "다시 들어와도 남아야 하는 메시지",
                  "type": "TALK",
                  "clientMessageId": "client-1"
                }
                """;

        ChatMessageDto message = objectMapper.readValue(payload, ChatMessageDto.class);

        assertThat(message.getRoomId()).isEqualTo(7L);
        assertThat(message.getSenderNo()).isEqualTo(1L);
        assertThat(message.getContent()).isEqualTo("다시 들어와도 남아야 하는 메시지");
        assertThat(message.getType()).isEqualTo("TALK");
        assertThat(message.getClientMessageId()).isEqualTo("client-1");
    }
}
