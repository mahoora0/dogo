package com.example.dogo.entity;

import com.example.dogo.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_MESSAGE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MESSAGE_ID")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROOM_ID", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SENDER_NO", nullable = false)
    private User sender;

    @Column(name = "CONTENT", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "MESSAGE_TYPE", nullable = false)
    private MessageType messageType = MessageType.TALK;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "IS_READ", nullable = false)
    private boolean read = false;

    @Column(name = "FILE_URL", length = 500)
    private String fileUrl;

    @Column(name = "FILE_NAME", length = 255)
    private String fileName;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    public enum MessageType {
        ENTER, TALK, FILE
    }

    public ChatMessage(ChatRoom chatRoom, User sender, String content, MessageType messageType) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(ChatRoom chatRoom, User sender, String content, MessageType messageType, String fileUrl, String fileName, Long fileSize) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.messageType = messageType;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.createdAt = LocalDateTime.now();
    }
}
