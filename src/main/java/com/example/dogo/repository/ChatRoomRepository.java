package com.example.dogo.repository;

import com.example.dogo.entity.ChatRoom;
import com.example.dogo.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.inquirer = :user OR cr.owner = :user")
    List<ChatRoom> findByParticipant(@Param("user") User user);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.foundItem.foundId = :foundId AND cr.inquirer = :inquirer")
    Optional<ChatRoom> findByFoundItemAndInquirer(@Param("foundId") Long foundId, @Param("inquirer") User inquirer);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.lostItem.lostId = :lostId AND cr.inquirer = :inquirer")
    Optional<ChatRoom> findByLostItemAndInquirer(@Param("lostId") Long lostId, @Param("inquirer") User inquirer);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.animalReport.reportId = :reportId AND cr.inquirer = :inquirer")
    Optional<ChatRoom> findByAnimalReportAndInquirer(@Param("reportId") Long reportId, @Param("inquirer") User inquirer);

    @Modifying
    @Query("DELETE FROM ChatRoom cr WHERE cr.inquirer = :user OR cr.owner = :user")
    void deleteByParticipant(@Param("user") User user);
}
