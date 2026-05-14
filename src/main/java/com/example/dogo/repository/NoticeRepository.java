package com.example.dogo.repository;

import com.example.dogo.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findByDeletedOrderByCreatedAtDesc(String deleted, Pageable pageable);
    Page<Notice> findByCategoryAndDeletedOrderByCreatedAtDesc(String category, String deleted, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
