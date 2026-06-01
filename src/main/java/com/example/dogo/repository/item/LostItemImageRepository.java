package com.example.dogo.repository.item;

import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.item.LostItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LostItemImageRepository extends JpaRepository<LostItemImage, Long> {

	Optional<LostItemImage> findFirstByLostItemOrderBySortOrderAscImageIdAsc(LostItem lostItem);

	List<LostItemImage> findByLostItemOrderBySortOrderAscImageIdAsc(LostItem lostItem);

	List<LostItemImage> findByLostItemInOrderBySortOrderAscImageIdAsc(List<LostItem> lostItems);
}
