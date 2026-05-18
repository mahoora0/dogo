package com.example.dogo.controller.chat;

import com.example.dogo.dto.ChatMessageDto;
import com.example.dogo.dto.ChatRoomDto;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.ChatService;
import com.example.dogo.service.chat.ChatUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;

    @GetMapping("/chat")
    public String chat(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        
        List<ChatRoomDto> rooms = chatService.getChatRooms(userDetails.getUser());
        model.addAttribute("rooms", rooms);
        model.addAttribute("currentUser", userDetails.getUser());
        model.addAttribute("currentUri", "/chat");
        return "chat/index";
    }

    @PostMapping("/chat/room")
    public String createRoom(@RequestParam Long itemId, 
                             @RequestParam String itemType, 
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";

        try {
            Long roomId = chatService.createOrGetRoom(itemId, itemType, userDetails.getUser());
            return "redirect:/chat?roomId=" + roomId;
        } catch (ChatUnavailableException e) {
            redirectAttributes.addFlashAttribute("chatError", e.getMessage());
            return "redirect:" + itemDetailPath(itemType, itemId);
        }
    }

    private String itemDetailPath(String itemType, Long itemId) {
        if ("FOUND".equals(itemType)) {
            return "/found-items/" + itemId;
        }
        return "/lost-items/" + itemId;
    }

    @GetMapping("/chat/room/{roomId}/messages")
    @ResponseBody
    public List<ChatMessageDto> getMessages(@PathVariable Long roomId) {
        return chatService.getChatMessages(roomId);
    }

    @PostMapping("/chat/room/{roomId}/messages")
    @ResponseBody
    public ChatMessageDto sendMessage(@PathVariable Long roomId,
                                      @RequestBody ChatMessageDto message,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ChatUnavailableException("로그인 후 채팅을 이용할 수 있습니다.");
        }

        ChatMessageDto request = ChatMessageDto.builder()
                .roomId(roomId)
                .senderNo(userDetails.getUser().getUserNo())
                .content(message.getContent())
                .type(message.getType())
                .clientMessageId(message.getClientMessageId())
                .build();
        ChatMessageDto savedMessage = chatService.saveMessage(request);
        publishMessage(savedMessage);
        return savedMessage;
    }

    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message) {
        ChatMessageDto savedMessage = chatService.saveMessage(message);
        publishMessage(savedMessage);
    }

    private void publishMessage(ChatMessageDto savedMessage) {
        messagingTemplate.convertAndSend("/sub/chat/room/" + savedMessage.getRoomId(), savedMessage);
        chatService.getChatParticipantUserNos(savedMessage.getRoomId()).stream()
                .filter(userNo -> !userNo.equals(savedMessage.getSenderNo()))
                .forEach(userNo -> messagingTemplate.convertAndSend("/sub/users/" + userNo + "/messages", savedMessage));
    }
}
