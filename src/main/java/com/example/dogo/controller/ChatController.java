package com.example.dogo.controller;

import com.example.dogo.dto.ChatMessageDto;
import com.example.dogo.dto.ChatRoomDto;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        
        Long roomId = chatService.createOrGetRoom(itemId, itemType, userDetails.getUser());
        return "redirect:/chat?roomId=" + roomId;
    }

    @GetMapping("/chat/room/{roomId}/messages")
    @ResponseBody
    public List<ChatMessageDto> getMessages(@PathVariable Long roomId) {
        return chatService.getChatMessages(roomId);
    }

    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message) {
        ChatMessageDto savedMessage = chatService.saveMessage(message);
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), savedMessage);
    }
}
