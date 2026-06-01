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
        when(room.getInquirer()).thenReturn(sender);
        when(sender.getUserNo()).thenReturn(senderNo);
        when(sender.getNickname()).thenReturn("테스터");

        // when
        ChatMessageDto result = chatService.saveMessage(dto, sender);

        // then
        assertEquals("안녕하세요", result.getContent());
        assertEquals("테스터", result.getSenderNickname());
        verify(chatMessageRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("작성자가 없는 습득물은 채팅방을 만들 수 없다")
    void saveMessageRejectsTooLongContent() {
        Long roomId = 1L;
        Long senderNo = 1L;
        ChatMessageDto dto = ChatMessageDto.builder()
                .roomId(roomId)
                .senderNo(senderNo)
                .content("a".repeat(1001))
                .type("TALK")
                .build();
        ChatRoom room = mock(ChatRoom.class);
        User sender = mock(User.class);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(room.getInquirer()).thenReturn(sender);
        when(sender.getUserNo()).thenReturn(senderNo);

        assertThrows(IllegalArgumentException.class, () -> chatService.saveMessage(dto, sender));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects chat messages over 1000 characters")
    void createOrGetRoomRejectsFoundItemWithoutOwner() {
        FoundItem foundItem = mock(FoundItem.class);
        User inquirer = mock(User.class);

        when(inquirer.getUserNo()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(inquirer));
        when(foundItemRepository.findById(10L)).thenReturn(Optional.of(foundItem));
        when(foundItem.getUser()).thenReturn(null);

        ChatUnavailableException exception = assertThrows(ChatUnavailableException.class,
                () -> chatService.createOrGetRoom(10L, "FOUND", inquirer));

        assertEquals("이 게시글은 채팅 신청이 불가능합니다.", exception.getMessage());
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상적인 정보로 1:1 채팅방 생성 성공")
    void createOrGetRoomSuccessfullyCreatesRoom() {
        FoundItem foundItem = mock(FoundItem.class);
        User inquirer = mock(User.class);
        User owner = mock(User.class);
        ChatRoom room = mock(ChatRoom.class);

        when(inquirer.getUserNo()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(inquirer));
        when(foundItemRepository.findById(10L)).thenReturn(Optional.of(foundItem));
        when(foundItem.getUser()).thenReturn(owner);
        when(owner.getUserNo()).thenReturn(2L);
        
        when(chatRoomRepository.findByFoundItemAndInquirer(10L, inquirer)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(room);
        when(room.getRoomId()).thenReturn(99L);

        Long roomId = chatService.createOrGetRoom(10L, "FOUND", inquirer);

        assertEquals(99L, roomId);
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
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
        Method method = ChatService.class.getMethod("getChatMessages", Long.class, User.class);
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
    @DisplayName("소유자(owner)이고 메시지가 없는 방은 목록에서 제외된다")
    void getChatRoomsFiltersOutEmptyRoomsForOwner() {
        User user = mock(User.class);
        User owner = mock(User.class);
        User inquirer = mock(User.class);
        ChatRoom room = mock(ChatRoom.class);

        when(user.getUserNo()).thenReturn(2L);
        when(inquirer.getUserNo()).thenReturn(1L);
        when(owner.getUserNo()).thenReturn(2L);
        when(room.getInquirer()).thenReturn(inquirer);
        when(room.getOwner()).thenReturn(owner);
        when(chatRoomRepository.findByParticipant(user)).thenReturn(List.of(room));
        when(chatMessageRepository.findTopByChatRoomOrderByCreatedAtDesc(room)).thenReturn(null);

        List<ChatRoomDto> rooms = chatService.getChatRooms(user);

        assertTrue(rooms.isEmpty());
    }

    @Test
    @DisplayName("신청자(inquirer)이고 메시지가 없는 방은 목록에 포함된다")
    void getChatRoomsIncludesEmptyRoomsForInquirer() {
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
        when(chatMessageRepository.findTopByChatRoomOrderByCreatedAtDesc(room)).thenReturn(null);
        when(owner.getNickname()).thenReturn("상대방");

        List<ChatRoomDto> rooms = chatService.getChatRooms(user);

        assertEquals(1, rooms.size());
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
        when(room.getInquirer()).thenReturn(user);
        when(user.getUserNo()).thenReturn(1L);

        chatService.markRoomMessagesAsRead(7L, user);

        verify(chatMessageRepository).markRoomMessagesAsRead(room, user);
    }

    @Test
    @DisplayName("파일 메시지 저장 테스트")
    void saveFileMessageTest() throws Exception {
        // given
        Long roomId = 1L;
        org.springframework.web.multipart.MultipartFile multipartFile = mock(org.springframework.web.multipart.MultipartFile.class);
        User sender = mock(User.class);
        ChatRoom room = mock(ChatRoom.class);

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test_file.txt");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(room.getInquirer()).thenReturn(sender);
        when(sender.getUserNo()).thenReturn(1L);
        when(sender.getNickname()).thenReturn("테스터");

        // when
        ChatMessageDto result = chatService.saveFileMessage(roomId, multipartFile, sender);

        // then
        assertEquals("[파일] test_file.txt", result.getContent());
        assertEquals("FILE", result.getType());
        assertEquals("test_file.txt", result.getFileName());
        assertEquals(1024L, result.getFileSize());
        assertTrue(result.getFileUrl().startsWith("/uploads/chats/"));
        verify(chatMessageRepository, times(1)).save(any());
        verify(multipartFile, times(1)).transferTo(any(java.nio.file.Path.class));
    }

    @Test
    @DisplayName("여러 이미지는 하나의 파일 묶음 메시지로 저장한다")
    void saveImageMessagesGroupsMultipleImages() throws Exception {
        Long roomId = 1L;
        org.springframework.web.multipart.MultipartFile first = mock(org.springframework.web.multipart.MultipartFile.class);
        org.springframework.web.multipart.MultipartFile second = mock(org.springframework.web.multipart.MultipartFile.class);
        User sender = mock(User.class);
        ChatRoom room = mock(ChatRoom.class);

        when(first.isEmpty()).thenReturn(false);
        when(first.getOriginalFilename()).thenReturn("first.png");
        when(first.getContentType()).thenReturn("image/png");
        when(first.getSize()).thenReturn(100L);
        when(second.isEmpty()).thenReturn(false);
        when(second.getOriginalFilename()).thenReturn("second.jpg");
        when(second.getContentType()).thenReturn("image/jpeg");
        when(second.getSize()).thenReturn(200L);
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(room.getInquirer()).thenReturn(sender);
        when(sender.getUserNo()).thenReturn(1L);
        when(sender.getNickname()).thenReturn("sender");

        ChatMessageDto result = chatService.saveImageMessages(roomId, List.of(first, second), sender);

        assertEquals("FILE_GROUP", result.getType());
        assertEquals(2, result.getFiles().size());
        assertEquals(300L, result.getFileSize());
        assertTrue(result.getFileGroupId() != null && !result.getFileGroupId().isBlank());
        verify(chatMessageRepository, times(2)).save(any());
        verify(first, times(1)).transferTo(any(java.nio.file.Path.class));
        verify(second, times(1)).transferTo(any(java.nio.file.Path.class));
    }

    @Test
    @DisplayName("채팅방 비참여자는 메시지를 저장할 수 없다")
    void saveMessageRejectsNonParticipant() {
        ChatMessageDto dto = ChatMessageDto.builder()
                .roomId(7L)
                .content("침입 메시지")
                .type("TALK")
                .build();
        User outsider = mock(User.class);
        User inquirer = mock(User.class);
        User owner = mock(User.class);
        ChatRoom room = mock(ChatRoom.class);

        when(outsider.getUserNo()).thenReturn(3L);
        when(inquirer.getUserNo()).thenReturn(1L);
        when(owner.getUserNo()).thenReturn(2L);
        when(room.getInquirer()).thenReturn(inquirer);
        when(room.getOwner()).thenReturn(owner);
        when(chatRoomRepository.findById(7L)).thenReturn(Optional.of(room));

        assertThrows(ChatUnavailableException.class, () -> chatService.saveMessage(dto, outsider));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅방 비참여자는 STOMP 방 채널을 구독할 수 없다")
    void authorizeSubscriptionRejectsNonParticipantRoomChannel() {
        User outsider = mock(User.class);
        User inquirer = mock(User.class);
        User owner = mock(User.class);
        ChatRoom room = mock(ChatRoom.class);

        when(userRepository.findByEmail("outsider@example.com")).thenReturn(Optional.of(outsider));
        when(outsider.getUserNo()).thenReturn(3L);
        when(inquirer.getUserNo()).thenReturn(1L);
        when(owner.getUserNo()).thenReturn(2L);
        when(room.getInquirer()).thenReturn(inquirer);
        when(room.getOwner()).thenReturn(owner);
        when(chatRoomRepository.findById(7L)).thenReturn(Optional.of(room));

        assertThrows(ChatUnavailableException.class,
                () -> chatService.authorizeSubscription("/sub/chat/room/7", "outsider@example.com"));
    }

    @Test
    @DisplayName("사용자는 다른 사용자의 STOMP 알림 채널을 구독할 수 없다")
    void authorizeSubscriptionRejectsOtherUserNotificationChannel() {
        User user = mock(User.class);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(user.getUserNo()).thenReturn(1L);

        assertThrows(ChatUnavailableException.class,
                () -> chatService.authorizeSubscription("/sub/users/2/messages", "user@example.com"));
    }
}
