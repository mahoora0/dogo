package com.example.dogo.repository;

import com.example.dogo.entity.ChatMessage;
import com.example.dogo.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
    ChatMessage findTopByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
}
