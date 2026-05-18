package com.example.dogo.entity;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_ROOM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROOM_ID")
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FOUND_ID")
    private FoundItem foundItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOST_ID")
    private LostItem lostItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INQUIRER_NO")
    private User inquirer; // 채팅 신청자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OWNER_NO")
    private User owner; // 게시글 작성자

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatRoom(FoundItem foundItem, User inquirer, User owner) {
        this.foundItem = foundItem;
        this.inquirer = inquirer;
        this.owner = owner;
        this.createdAt = LocalDateTime.now();
    }

    public ChatRoom(LostItem lostItem, User inquirer, User owner) {
        this.lostItem = lostItem;
        this.inquirer = inquirer;
        this.owner = owner;
        this.createdAt = LocalDateTime.now();
    }
}
