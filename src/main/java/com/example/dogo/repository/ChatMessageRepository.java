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
    int countByChatRoomAndSenderNotAndReadFalse(ChatRoom chatRoom, User sender);
    int countByChatRoomInAndSenderNotAndReadFalse(List<ChatRoom> chatRooms, User sender);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true WHERE m.chatRoom = :chatRoom AND m.sender <> :reader AND m.read = false")
    void markRoomMessagesAsRead(@Param("chatRoom") ChatRoom chatRoom, @Param("reader") User reader);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.chatRoom IN (SELECT cr FROM ChatRoom cr WHERE cr.inquirer = :user OR cr.owner = :user)")
    void deleteByParticipant(@Param("user") User user);
}
