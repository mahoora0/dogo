package com.example.dogo.repository.Support;

import com.example.dogo.entity.Support.Guide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Long> {
    Page<Guide> findByDeletedOrderByCreatedAtDesc(String deleted, Pageable pageable);
    Page<Guide> findByCategoryAndDeletedOrderByCreatedAtDesc(String category, String deleted, Pageable pageable);
}
