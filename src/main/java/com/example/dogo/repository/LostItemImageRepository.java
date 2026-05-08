package com.example.dogo.repository;

import com.example.dogo.entity.LostItem;
import com.example.dogo.entity.LostItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LostItemImageRepository extends JpaRepository<LostItemImage, Long> {

	Optional<LostItemImage> findFirstByLostItemOrderBySortOrderAscImageIdAsc(LostItem lostItem);

	List<LostItemImage> findByLostItemOrderBySortOrderAscImageIdAsc(LostItem lostItem);
}
