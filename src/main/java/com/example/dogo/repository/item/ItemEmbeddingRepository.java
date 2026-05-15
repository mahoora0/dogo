package com.example.dogo.repository.item;

import com.example.dogo.entity.item.ItemEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemEmbeddingRepository extends JpaRepository<ItemEmbedding, Long> {

	Optional<ItemEmbedding> findByItemTypeAndItemId(String itemType, Long itemId);

	@Query("SELECT e FROM ItemEmbedding e WHERE e.itemType = :itemType AND e.itemId IN :itemIds")
	List<ItemEmbedding> findByItemTypeAndItemIdIn(@Param("itemType") String itemType,
			@Param("itemIds") Collection<Long> itemIds);
}
