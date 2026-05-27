package com.example.dogo.service;

import com.example.dogo.dto.ChatFileDto;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String CHAT_UNAVAILABLE_MESSAGE = "이 게시글은 채팅 신청이 불가능합니다.";

    private static final int MAX_MESSAGE_CONTENT_LENGTH = 1000;

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
                    String lastMessage = lastMsg != null ? lastMsg.getContent() : "\ub300\ud654\ub97c \uc2dc\uc791\ud574\ubcf4\uc138\uc694.";

                    if (lastMsg != null && StringUtils.hasText(lastMsg.getFileGroupId())) {
                        int groupedFileCount = chatMessageRepository.countByChatRoomAndFileGroupId(room, lastMsg.getFileGroupId());
                        lastMessage = "\u005b\uc774\ubbf8\uc9c0\u005d " + groupedFileCount + "\uac1c";
                    }

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
                            .lastMessage(lastMessage)
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
        List<ChatMessageDto> result = new ArrayList<>();
        Set<String> renderedGroups = new LinkedHashSet<>();

        for (ChatMessage message : messages) {
            String fileGroupId = message.getFileGroupId();
            if (StringUtils.hasText(fileGroupId)) {
                if (renderedGroups.add(fileGroupId)) {
                    List<ChatMessage> groupedMessages = messages.stream()
                            .filter(candidate -> fileGroupId.equals(candidate.getFileGroupId()))
                            .collect(Collectors.toList());
                    result.add(toFileGroupDto(roomId, fileGroupId, groupedMessages));
                }
                continue;
            }

            result.add(toMessageDto(roomId, message));
        }

        return result;
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
        String content = normalizeMessageContent(dto.getContent());
        
        ChatMessage message = new ChatMessage(
                room,
                sender,
                content,
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
                .content(content)
                .type(dto.getType())
                .createdAt(message.getCreatedAt())
                .clientMessageId(dto.getClientMessageId())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .fileGroupId(message.getFileGroupId())
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

    @Transactional
    public ChatMessageDto saveImageMessages(Long roomId, List<MultipartFile> files, User sender) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found."));

        List<MultipartFile> uploadFiles = files == null ? List.of() : files.stream()
                .filter(candidate -> candidate != null && !candidate.isEmpty())
                .collect(Collectors.toList());

        if (uploadFiles.isEmpty()) {
            throw new IllegalArgumentException("Upload files are empty.");
        }

        try {
            Files.createDirectories(chatUploadPath);

            String fileGroupId = uploadFiles.size() > 1 ? UUID.randomUUID().toString() : null;
            List<ChatMessage> savedMessages = new ArrayList<>();

            for (MultipartFile file : uploadFiles) {
                if (!isImageUpload(file)) {
                    throw new IllegalArgumentException("Only image files can be attached.");
                }

                ChatFileDto storedFile = storeChatFile(file);
                ChatMessage message = new ChatMessage(
                        room,
                        sender,
                        "\u005b\ud30c\uc77c\u005d " + storedFile.getFileName(),
                        ChatMessage.MessageType.FILE,
                        storedFile.getFileUrl(),
                        storedFile.getFileName(),
                        storedFile.getFileSize(),
                        fileGroupId
                );
                chatMessageRepository.save(message);
                savedMessages.add(message);
            }

            if (StringUtils.hasText(fileGroupId)) {
                return toFileGroupDto(roomId, fileGroupId, savedMessages);
            }
            return toMessageDto(roomId, savedMessages.get(0));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save chat files.", exception);
        }
    }

    private ChatFileDto storeChatFile(MultipartFile file) throws IOException {
        String originalName = StringUtils.cleanPath(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload");
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + extension;
        Path targetPath = chatUploadPath.resolve(storedName).normalize();

        if (!targetPath.startsWith(chatUploadPath)) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        file.transferTo(targetPath);

        return ChatFileDto.builder()
                .fileUrl("/uploads/chats/" + storedName)
                .fileName(originalName)
                .fileSize(file.getSize())
                .build();
    }

    private ChatMessageDto toMessageDto(Long roomId, ChatMessage message) {
        return ChatMessageDto.builder()
                .roomId(roomId)
                .senderNo(message.getSender().getUserNo())
                .senderNickname(message.getSender().getNickname())
                .senderProfileImage(message.getSender().getProfileImageUrl())
                .content(message.getContent())
                .type(message.getMessageType().name())
                .createdAt(message.getCreatedAt())
                .clientMessageId(null)
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .fileGroupId(message.getFileGroupId())
                .build();
    }

    private ChatMessageDto toFileGroupDto(Long roomId, String fileGroupId, List<ChatMessage> messages) {
        ChatMessage first = messages.get(0);
        List<ChatFileDto> files = messages.stream()
                .map(this::toChatFileDto)
                .collect(Collectors.toList());
        long totalSize = files.stream()
                .map(ChatFileDto::getFileSize)
                .filter(size -> size != null)
                .mapToLong(Long::longValue)
                .sum();

        return ChatMessageDto.builder()
                .roomId(roomId)
                .senderNo(first.getSender().getUserNo())
                .senderNickname(first.getSender().getNickname())
                .senderProfileImage(first.getSender().getProfileImageUrl())
                .content("\u005b\uc774\ubbf8\uc9c0\u005d " + files.size() + "\uac1c")
                .type("FILE_GROUP")
                .createdAt(first.getCreatedAt())
                .clientMessageId(null)
                .fileUrl(first.getFileUrl())
                .fileName(first.getFileName())
                .fileSize(totalSize)
                .fileGroupId(fileGroupId)
                .files(files)
                .build();
    }

    private ChatFileDto toChatFileDto(ChatMessage message) {
        return ChatFileDto.builder()
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .build();
    }

    private boolean isImageUpload(MultipartFile file) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        return isImageFileName(file.getOriginalFilename());
    }

    private boolean isImageFileName(String filename) {
        String extension = extractExtension(filename);
        return List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg").contains(extension);
    }

    private String normalizeMessageContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Message content is empty.");
        }
        if (normalized.length() > MAX_MESSAGE_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Message content is too long.");
        }
        return normalized;
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
