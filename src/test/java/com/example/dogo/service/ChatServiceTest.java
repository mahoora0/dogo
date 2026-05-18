package com.example.dogo.service;

import com.example.dogo.dto.ChatMessageDto;
import com.example.dogo.dto.ChatRoomDto;
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
import java.util.List;
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

    @Test
    @DisplayName("채팅방 목록은 상대가 보낸 읽지 않은 메시지 수를 포함한다")
    void getChatRoomsIncludesUnreadMessagesFromOtherParticipant() {
        User user = mock(User.class);
        User owner = mock(User.class);
        User inquirer = mock(User.class);
        ChatRoom room = mock(ChatRoom.class);

        when(user.getUserNo()).thenReturn(1L);
        when(inquirer.getUserNo()).thenReturn(1L);
        when(owner.getUserNo()).thenReturn(2L);
        when(room.getInquirer()).thenReturn(inquirer);
        when(room.getOwner()).thenReturn(owner);
        when(chatRoomRepository.findByParticipant(user)).thenReturn(List.of(room));
        when(chatMessageRepository.countByChatRoomAndSenderNotAndReadFalse(room, user)).thenReturn(3);
        when(owner.getNickname()).thenReturn("상대방");

        List<ChatRoomDto> rooms = chatService.getChatRooms(user);

        assertEquals(3, rooms.get(0).getUnreadCount());
    }

    @Test
    @DisplayName("전체 미확인 채팅 수는 참여 중인 모든 채팅방에서 상대 메시지만 합산한다")
    void getUnreadCountSumsUnreadMessagesFromOtherParticipants() {
        User user = mock(User.class);
        ChatRoom room1 = mock(ChatRoom.class);
        ChatRoom room2 = mock(ChatRoom.class);

        when(chatRoomRepository.findByParticipant(user)).thenReturn(List.of(room1, room2));
        when(chatMessageRepository.countByChatRoomInAndSenderNotAndReadFalse(List.of(room1, room2), user)).thenReturn(120);

        assertEquals(120, chatService.getUnreadCount(user));
    }

    @Test
    @DisplayName("읽지 않은 채팅이 없는 사용자는 전체 미확인 채팅 수가 0이다")
    void getUnreadCountReturnsZeroWithoutChatRooms() {
        User user = mock(User.class);

        when(chatRoomRepository.findByParticipant(user)).thenReturn(List.of());

        assertEquals(0, chatService.getUnreadCount(user));
        verify(chatMessageRepository, never()).countByChatRoomInAndSenderNotAndReadFalse(any(), any());
    }

    @Test
    @DisplayName("채팅방 메시지를 확인하면 상대가 보낸 메시지만 읽음 처리한다")
    void markRoomMessagesAsReadMarksOnlyOtherParticipantMessages() {
        User user = mock(User.class);
        ChatRoom room = mock(ChatRoom.class);

        when(chatRoomRepository.findById(7L)).thenReturn(Optional.of(room));

        chatService.markRoomMessagesAsRead(7L, user);

        verify(chatMessageRepository).markRoomMessagesAsRead(room, user);
    }
}
