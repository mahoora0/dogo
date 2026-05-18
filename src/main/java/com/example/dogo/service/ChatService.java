package com.example.dogo.service;

import com.example.dogo.dto.ChatMessageDto;
import com.example.dogo.dto.ChatRoomDto;
import com.example.dogo.entity.*;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.FoundItemImage;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.item.LostItemImage;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.*;
import com.example.dogo.repository.item.FoundItemImageRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemImageRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.service.chat.ChatUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String CHAT_UNAVAILABLE_MESSAGE = "이 게시글은 채팅 신청이 불가능합니다.";

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
            FoundItem item = foundItemRepository.findById(itemId)
                    .orElseThrow(() -> new ChatUnavailableException(CHAT_UNAVAILABLE_MESSAGE));
            validateChatAvailable(item.getUser(), inquirer);
            return chatRoomRepository.findByFoundItemAndInquirer(itemId, inquirer)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = new ChatRoom(item, inquirer, item.getUser());
                        return chatRoomRepository.save(room).getRoomId();
                    });
        } else if ("LOST".equals(itemType)) {
            LostItem item = lostItemRepository.findById(itemId)
                    .orElseThrow(() -> new ChatUnavailableException(CHAT_UNAVAILABLE_MESSAGE));
            validateChatAvailable(item.getUser(), inquirer);
            return chatRoomRepository.findByLostItemAndInquirer(itemId, inquirer)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = new ChatRoom(item, inquirer, item.getUser());
                        return chatRoomRepository.save(room).getRoomId();
                    });
        }

        throw new ChatUnavailableException(CHAT_UNAVAILABLE_MESSAGE);
    }

    private void validateChatAvailable(User owner, User inquirer) {
        if (owner == null || inquirer == null || isSameUser(owner, inquirer)) {
            throw new ChatUnavailableException(CHAT_UNAVAILABLE_MESSAGE);
        }
    }

    private boolean isSameUser(User owner, User inquirer) {
        if (owner.getUserNo() != null && inquirer.getUserNo() != null) {
            return owner.getUserNo().equals(inquirer.getUserNo());
        }
        return owner == inquirer;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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
                .clientMessageId(null)
                .build()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Long> getChatParticipantUserNos(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        return List.of(room.getInquirer().getUserNo(), room.getOwner().getUserNo());
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
                .clientMessageId(dto.getClientMessageId())
                .build();
    }
}
