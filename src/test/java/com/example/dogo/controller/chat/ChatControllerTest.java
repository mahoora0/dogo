package com.example.dogo.controller.chat;

import com.example.dogo.entity.user.User;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.ChatService;
import com.example.dogo.service.chat.ChatUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerTest {

    private final ChatService chatService = mock(ChatService.class);
    private final SimpMessageSendingOperations messagingTemplate = mock(SimpMessageSendingOperations.class);
    private final ChatController controller = new ChatController(chatService, messagingTemplate);

    @Test
    @DisplayName("채팅 신청이 불가능하면 에러 페이지 대신 상세 페이지로 안내 메시지와 함께 돌아간다")
    void createRoomRedirectsBackWithMessageWhenChatUnavailable() {
        User user = new User("user@example.com", "신청자", "010-0000-0000");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        when(chatService.createOrGetRoom(10L, "FOUND", user))
                .thenThrow(new ChatUnavailableException("이 게시글은 채팅 신청이 불가능합니다."));

        String view = controller.createRoom(10L, "FOUND", userDetails, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/found-items/10");
        verify(redirectAttributes).addFlashAttribute("chatError", "이 게시글은 채팅 신청이 불가능합니다.");
    }

    @Test
    @DisplayName("메시지는 방 구독 채널과 상대방 사용자 채널 모두로 전달한다")
    void messagePublishesToRoomAndOtherParticipants() {
        com.example.dogo.dto.ChatMessageDto message = com.example.dogo.dto.ChatMessageDto.builder()
                .roomId(7L)
                .senderNo(1L)
                .content("안녕하세요")
                .type("TALK")
                .build();
        com.example.dogo.dto.ChatMessageDto saved = com.example.dogo.dto.ChatMessageDto.builder()
                .roomId(7L)
                .senderNo(1L)
                .content("안녕하세요")
                .type("TALK")
                .build();

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("sender");
        when(chatService.saveMessage(message, "sender")).thenReturn(saved);
        when(chatService.getChatParticipantUserNos(7L)).thenReturn(java.util.List.of(1L, 2L));

        controller.message(message, principal);

        verify(messagingTemplate).convertAndSend("/sub/chat/room/7", saved);
        verify(messagingTemplate).convertAndSend(eq("/sub/users/2/messages"), eq(saved));
    }

    @Test
    @DisplayName("HTTP 메시지 전송도 저장 후 방 채널과 상대방 사용자 채널 모두로 전달한다")
    void sendMessagePersistsAndPublishesMessage() {
        User user = new User("sender@example.com", "보낸사람", "010-0000-0000");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        com.example.dogo.dto.ChatMessageDto request = com.example.dogo.dto.ChatMessageDto.builder()
                .roomId(7L)
                .senderNo(999L)
                .content("이전 대화도 남아야 해요")
                .type("TALK")
                .clientMessageId("client-1")
                .build();
        com.example.dogo.dto.ChatMessageDto saved = com.example.dogo.dto.ChatMessageDto.builder()
                .roomId(7L)
                .senderNo(user.getUserNo())
                .content("이전 대화도 남아야 해요")
                .type("TALK")
                .clientMessageId("client-1")
                .build();

        when(chatService.saveMessage(org.mockito.ArgumentMatchers.any(), eq(user))).thenReturn(saved);
        when(chatService.getChatParticipantUserNos(7L)).thenReturn(java.util.List.of(1L, 2L));

        com.example.dogo.dto.ChatMessageDto response = controller.sendMessage(7L, request, userDetails);

        assertThat(response).isEqualTo(saved);
        verify(messagingTemplate).convertAndSend("/sub/chat/room/7", saved);
        verify(messagingTemplate).convertAndSend(eq("/sub/users/2/messages"), eq(saved));
    }

    @Test
    @DisplayName("채팅방 메시지 조회 시 현재 사용자의 상대 메시지를 읽음 처리한다")
    void getMessagesMarksRoomMessagesAsReadForCurrentUser() {
        User user = new User("reader@example.com", "읽는사람", "010-0000-0000");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        List<com.example.dogo.dto.ChatMessageDto> messages = List.of(
                com.example.dogo.dto.ChatMessageDto.builder()
                        .roomId(7L)
                        .senderNo(2L)
                        .content("새 메시지")
                        .type("TALK")
                        .build()
        );

        when(chatService.getChatMessages(7L, user)).thenReturn(messages);

        List<com.example.dogo.dto.ChatMessageDto> response = controller.getMessages(7L, userDetails);

        assertThat(response).isEqualTo(messages);
        verify(chatService).markRoomMessagesAsRead(7L, user);
    }

    @Test
    @DisplayName("HTTP 이미지 묶음 업로드는 한 메시지로 저장하고 배포한다")
    void uploadImagesPersistsAndPublishesGroupedMessage() {
        User user = new User("sender@example.com", "보낸사람", "010-0000-0000");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        com.example.dogo.dto.ChatMessageDto saved = com.example.dogo.dto.ChatMessageDto.builder()
                .roomId(7L)
                .senderNo(1L)
                .content("[이미지] 2개")
                .type("FILE_GROUP")
                .fileGroupId("group-1")
                .build();

        when(chatService.saveImageMessages(eq(7L), org.mockito.ArgumentMatchers.anyList(), eq(user))).thenReturn(saved);
        when(chatService.getChatParticipantUserNos(7L)).thenReturn(List.of(1L, 2L));

        com.example.dogo.dto.ChatMessageDto response = controller.uploadImages(7L, List.of(file), userDetails);

        assertThat(response).isEqualTo(saved);
        verify(messagingTemplate).convertAndSend("/sub/chat/room/7", saved);
        verify(messagingTemplate).convertAndSend(eq("/sub/users/2/messages"), eq(saved));
    }
}
