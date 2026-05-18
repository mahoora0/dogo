package com.example.dogo.service;

import com.example.dogo.dto.ChatMessageDto;
import com.example.dogo.entity.ChatRoom;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.*;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.service.chat.ChatUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Mock
    private FoundItemRepository foundItemRepository;

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

    @Test
    @DisplayName("작성자가 없는 습득물은 채팅방을 만들 수 없다")
    void createOrGetRoomRejectsFoundItemWithoutOwner() {
        FoundItem foundItem = mock(FoundItem.class);
        User inquirer = mock(User.class);

        when(foundItemRepository.findById(10L)).thenReturn(Optional.of(foundItem));
        when(foundItem.getUser()).thenReturn(null);

        ChatUnavailableException exception = assertThrows(ChatUnavailableException.class,
                () -> chatService.createOrGetRoom(10L, "FOUND", inquirer));

        assertEquals("이 게시글은 채팅 신청이 불가능합니다.", exception.getMessage());
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅방 목록 조회는 지연 로딩을 처리할 수 있도록 읽기 트랜잭션에서 실행한다")
    void getChatRoomsRunsInReadOnlyTransaction() throws Exception {
        Method method = ChatService.class.getMethod("getChatRooms", User.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertTrue(transactional != null && transactional.readOnly());
    }

    @Test
    @DisplayName("저장된 메시지 이력 조회는 지연 로딩을 처리할 수 있도록 읽기 트랜잭션에서 실행한다")
    void getChatMessagesRunsInReadOnlyTransaction() throws Exception {
        Method method = ChatService.class.getMethod("getChatMessages", Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertTrue(transactional != null && transactional.readOnly());
    }
}
