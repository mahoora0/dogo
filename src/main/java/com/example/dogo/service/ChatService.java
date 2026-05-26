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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.Locale;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String CHAT_UNAVAILABLE_MESSAGE = "이 게시글은 채팅 신청이 불가능합니다.";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final FoundItemRepository foundItemRepository;
    private final LostItemRepository lostItemRepository;
    private final UserRepository userRepository;
    private final FoundItemImageRepository foundItemImageRepository;
    private final LostItemImageRepository lostItemImageRepository;
    private final Path chatUploadPath;

    public ChatService(
            ChatRoomRepository chatRoomRepository,
            ChatMessageRepository chatMessageRepository,
            FoundItemRepository foundItemRepository,
            LostItemRepository lostItemRepository,
            UserRepository userRepository,
            FoundItemImageRepository foundItemImageRepository,
            LostItemImageRepository lostItemImageRepository,
            @Value("${file.upload-dir}") String uploadDir) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.foundItemRepository = foundItemRepository;
        this.lostItemRepository = lostItemRepository;
        this.userRepository = userRepository;
        this.foundItemImageRepository = foundItemImageRepository;
        this.lostItemImageRepository = lostItemImageRepository;
        this.chatUploadPath = uploadDir != null ? Path.of(uploadDir, "chats").toAbsolutePath().normalize() : Path.of(System.getProperty("java.io.tmpdir"), "chats").toAbsolutePath().normalize();
    }

    @Transactional
    public Long createOrGetRoom(Long itemId, String itemType, User inquirer) {
        if (inquirer == null || inquirer.getUserNo() == null) {
            throw new ChatUnavailableException("로그인 후 이용할 수 있습니다.");
        }
        User managedInquirer = userRepository.findById(inquirer.getUserNo())
                .orElseThrow(() -> new ChatUnavailableException("사용자를 찾을 수 없습니다."));

        if ("FOUND".equals(itemType)) {
            FoundItem item = foundItemRepository.findById(itemId)
                    .orElseThrow(() -> new ChatUnavailableException(CHAT_UNAVAILABLE_MESSAGE));
            validateChatAvailable(item.getUser(), managedInquirer);
            return chatRoomRepository.findByFoundItemAndInquirer(itemId, managedInquirer)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = new ChatRoom(item, managedInquirer, item.getUser());
                        return chatRoomRepository.save(room).getRoomId();
                    });
        } else if ("LOST".equals(itemType)) {
            LostItem item = lostItemRepository.findById(itemId)
                    .orElseThrow(() -> new ChatUnavailableException(CHAT_UNAVAILABLE_MESSAGE));
            validateChatAvailable(item.getUser(), managedInquirer);
            return chatRoomRepository.findByLostItemAndInquirer(itemId, managedInquirer)
                    .map(ChatRoom::getRoomId)
                    .orElseGet(() -> {
                        ChatRoom room = new ChatRoom(item, managedInquirer, item.getUser());
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

    private static final java.time.format.DateTimeFormatter CHAT_DATE_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Transactional(readOnly = true)
    public List<ChatRoomDto> getChatRooms(User user) {
        List<ChatRoom> rooms = chatRoomRepository.findByParticipant(user);
        return rooms.stream()
                .map(room -> {
                    User other = room.getInquirer().getUserNo().equals(user.getUserNo()) ? room.getOwner() : room.getInquirer();
                    ChatMessage lastMsg = chatMessageRepository.findTopByChatRoomOrderByCreatedAtDesc(room);
                    
                    // 만약 현재 사용자가 방의 소유자(owner)이고, 마지막 메시지가 없다면 (신청자가 첫 메시지를 전송하기 전) 리스트에서 제외
                    if (room.getOwner().getUserNo().equals(user.getUserNo()) && lastMsg == null) {
                        return null;
                    }

                    String title = "";
                    String thumbnail = "";
                    Long itemId = null;
                    String type = "";
                    String place = "";
                    String dateStr = "";

                    if (room.getFoundItem() != null) {
                        title = room.getFoundItem().getTitle();
                        itemId = room.getFoundItem().getFoundId();
                        type = "FOUND";
                        place = room.getFoundItem().getFoundPlace();
                        if (room.getFoundItem().getFoundAt() != null) {
                            dateStr = room.getFoundItem().getFoundAt().format(CHAT_DATE_FORMATTER);
                        }
                        List<FoundItemImage> images = foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(room.getFoundItem());
                        if (!images.isEmpty()) thumbnail = images.get(0).getImageUrl();
                    } else if (room.getLostItem() != null) {
                        title = room.getLostItem().getTitle();
                        itemId = room.getLostItem().getLostId();
                        type = "LOST";
                        place = room.getLostItem().getLostPlace();
                        if (room.getLostItem().getLostAt() != null) {
                            dateStr = room.getLostItem().getLostAt().format(CHAT_DATE_FORMATTER);
                        }
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
                            .unreadCount(chatMessageRepository.countByChatRoomAndSenderNotAndReadFalse(room, user))
                            .itemId(itemId)
                            .itemType(type)
                            .itemPlace(place)
                            .itemDate(dateStr)
                            .build();
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(User user) {
        List<ChatRoom> rooms = chatRoomRepository.findByParticipant(user);
        if (rooms.isEmpty()) {
            return 0;
        }
        return chatMessageRepository.countByChatRoomInAndSenderNotAndReadFalse(rooms, user);
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
                .fileUrl(msg.getFileUrl())
                .fileName(msg.getFileName())
                .fileSize(msg.getFileSize())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public void markRoomMessagesAsRead(Long roomId, User reader) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        chatMessageRepository.markRoomMessagesAsRead(room, reader);
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
        
        ChatMessage message = new ChatMessage(
                room,
                sender,
                dto.getContent(),
                ChatMessage.MessageType.valueOf(dto.getType()),
                dto.getFileUrl(),
                dto.getFileName(),
                dto.getFileSize()
        );
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
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .build();
    }

    @Transactional
    public ChatMessageDto saveFileMessage(Long roomId, MultipartFile file, User sender) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
        }

        try {
            Files.createDirectories(chatUploadPath);

            String originalName = StringUtils.cleanPath(String.valueOf(file.getOriginalFilename()));
            String extension = extractExtension(originalName);
            String storedName = UUID.randomUUID() + extension;
            Path targetPath = chatUploadPath.resolve(storedName).normalize();
            
            if (!targetPath.startsWith(chatUploadPath)) {
                throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
            }

            file.transferTo(targetPath);

            String fileUrl = "/uploads/chats/" + storedName;
            String content = "[파일] " + originalName;

            ChatMessage message = new ChatMessage(
                    room,
                    sender,
                    content,
                    ChatMessage.MessageType.FILE,
                    fileUrl,
                    originalName,
                    file.getSize()
            );
            chatMessageRepository.save(message);

            return ChatMessageDto.builder()
                    .roomId(roomId)
                    .senderNo(sender.getUserNo())
                    .senderNickname(sender.getNickname())
                    .senderProfileImage(sender.getProfileImageUrl())
                    .content(content)
                    .type(ChatMessage.MessageType.FILE.name())
                    .createdAt(message.getCreatedAt())
                    .clientMessageId(null)
                    .fileUrl(fileUrl)
                    .fileName(originalName)
                    .fileSize(file.getSize())
                    .build();
        } catch (IOException exception) {
            throw new UncheckedIOException("파일 저장에 실패했습니다.", exception);
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        if (extension.length() > 12) {
            return "";
        }
        return extension;
    }
}
