package com.example.dogo.service;

import com.example.dogo.dto.ChatMessageDto;
import com.example.dogo.entity.ChatRoom;
import com.example.dogo.entity.User;
import com.example.dogo.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("메시지 저장 테스트")
    void saveMessageTest() {
        // given
        Long roomId = 1L;
        Long senderNo = 1L;
        ChatMessageDto dto = ChatMessageDto.builder()
                .roomId(roomId)
                .senderNo(senderNo)
                .content("안녕하세요")
                .type("TALK")
                .build();

        ChatRoom room = mock(ChatRoom.class);
        User sender = mock(User.class);
        
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(userRepository.findById(senderNo)).thenReturn(Optional.of(sender));
        when(sender.getUserNo()).thenReturn(senderNo);
        when(sender.getNickname()).thenReturn("테스터");

        // when
        ChatMessageDto result = chatService.saveMessage(dto);

        // then
        assertEquals("안녕하세요", result.getContent());
        assertEquals("테스터", result.getSenderNickname());
        verify(chatMessageRepository, times(1)).save(any());
    }
}
