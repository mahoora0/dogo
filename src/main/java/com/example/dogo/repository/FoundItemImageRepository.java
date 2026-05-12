package com.example.dogo.repository;

import com.example.dogo.entity.FoundItem;
import com.example.dogo.entity.FoundItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoundItemImageRepository extends JpaRepository<FoundItemImage, Long> {

	Optional<FoundItemImage> findFirstByFoundItemOrderBySortOrderAscImageIdAsc(FoundItem foundItem);

	List<FoundItemImage> findByFoundItemOrderBySortOrderAscImageIdAsc(FoundItem foundItem);
}
