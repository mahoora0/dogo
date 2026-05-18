package com.example.dogo.repository;

import com.example.dogo.entity.ChatMessage;
import com.example.dogo.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.dogo.entity.user.User;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
    ChatMessage findTopByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.chatRoom IN (SELECT cr FROM ChatRoom cr WHERE cr.inquirer = :user OR cr.owner = :user)")
    void deleteByParticipant(@Param("user") User user);
}
