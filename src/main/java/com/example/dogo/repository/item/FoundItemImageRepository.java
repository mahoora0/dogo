package com.example.dogo.repository.item;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.FoundItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoundItemImageRepository extends JpaRepository<FoundItemImage, Long> {

	Optional<FoundItemImage> findFirstByFoundItemOrderBySortOrderAscImageIdAsc(FoundItem foundItem);

	List<FoundItemImage> findByFoundItemOrderBySortOrderAscImageIdAsc(FoundItem foundItem);

	List<FoundItemImage> findByFoundItemInOrderBySortOrderAscImageIdAsc(List<FoundItem> foundItems);
}
