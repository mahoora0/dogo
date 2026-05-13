package com.example.dogo.service;

import com.example.dogo.dto.ChatMessageDto;
import com.example.dogo.dto.ChatRoomDto;
import com.example.dogo.entity.*;
import com.example.dogo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final FoundItemRepository foundItemRepository;
    private final LostItemRepository lostItemRepository;
    private final UserRepository userRepository;
    private final FoundItemImageRepository foundItemImageRepository;
    private final LostItemImageRepository lostItemImageRepository;

    @Transactional
    public Long createOrGetRoom(Long itemId, String itemType, User inquirer) {
        if ("FOUND".equals(itemType)) {
            FoundItem item = foundItemRepository.findById(itemId).orElseThrow();
            return chatRoomRepository.findByFoundItemAndInquirer(itemId, inquirer)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = new ChatRoom(item, inquirer, item.getUser());
                        return chatRoomRepository.save(room).getRoomId();
                    });
        } else {
            LostItem item = lostItemRepository.findById(itemId).orElseThrow();
            return chatRoomRepository.findByLostItemAndInquirer(itemId, inquirer)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = new ChatRoom(item, inquirer, item.getUser());
                        return chatRoomRepository.save(room).getRoomId();
                    });
        }
    }

    public List<ChatRoomDto> getChatRooms(User user) {
        List<ChatRoom> rooms = chatRoomRepository.findByParticipant(user);
        return rooms.stream().map(room -> {
            User other = room.getInquirer().getUserNo().equals(user.getUserNo()) ? room.getOwner() : room.getInquirer();
            ChatMessage lastMsg = chatMessageRepository.findTopByChatRoomOrderByCreatedAtDesc(room);
            
            String title = "";
            String thumbnail = "";
            Long itemId = null;
            String type = "";

            if (room.getFoundItem() != null) {
                title = room.getFoundItem().getTitle();
                itemId = room.getFoundItem().getFoundId();
                type = "FOUND";
                List<FoundItemImage> images = foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(room.getFoundItem());
                if (!images.isEmpty()) thumbnail = images.get(0).getImageUrl();
            } else if (room.getLostItem() != null) {
                title = room.getLostItem().getTitle();
                itemId = room.getLostItem().getLostId();
                type = "LOST";
                List<LostItemImage> images = lostItemImageRepository.findByLostItemOrderBySortOrderAscImageIdAsc(room.getLostItem());
                if (!images.isEmpty()) thumbnail = images.get(0).getImageUrl();
            }

            return ChatRoomDto.builder()
                    .roomId(room.getRoomId())
                    .itemTitle(title)
                    .itemThumbnail(thumbnail)
                    .lastMessage(lastMsg != null ? lastMsg.getContent() : "대화를 시작해보세요.")
                    .lastMessageTime(lastMsg != null ? lastMsg.getCreatedAt() : room.getCreatedAt())
                    .otherParticipantNickname(other.getNickname())
                    .otherParticipantProfileImage(other.getProfileImageUrl())
                    .otherParticipantNo(other.getUserNo())
                    .itemId(itemId)
                    .itemType(type)
                    .build();
        }).collect(Collectors.toList());
    }

    public List<ChatMessageDto> getChatMessages(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtAsc(room);
        return messages.stream().map(msg -> ChatMessageDto.builder()
                .roomId(roomId)
                .senderNo(msg.getSender().getUserNo())
                .senderNickname(msg.getSender().getNickname())
                .senderProfileImage(msg.getSender().getProfileImageUrl())
                .content(msg.getContent())
                .type(msg.getMessageType().name())
                .createdAt(msg.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto saveMessage(ChatMessageDto dto) {
        ChatRoom room = chatRoomRepository.findById(dto.getRoomId()).orElseThrow();
        User sender = userRepository.findById(dto.getSenderNo()).orElseThrow();
        
        ChatMessage message = new ChatMessage(room, sender, dto.getContent(), ChatMessage.MessageType.valueOf(dto.getType()));
        chatMessageRepository.save(message);
        
        return ChatMessageDto.builder()
                .roomId(dto.getRoomId())
                .senderNo(sender.getUserNo())
                .senderNickname(sender.getNickname())
                .senderProfileImage(sender.getProfileImageUrl())
                .content(dto.getContent())
                .type(dto.getType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
