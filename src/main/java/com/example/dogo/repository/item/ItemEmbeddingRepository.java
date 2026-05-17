package com.example.dogo.repository.item;

import com.example.dogo.entity.item.ItemEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

	@Modifying
	@Query(value = """
			INSERT INTO ITEM_EMBEDDING (
				ITEM_TYPE, ITEM_ID, EMBEDDING_TEXT, TEXT_HASH, MODEL_NAME, VECTOR_BLOB, CREATED_AT, UPDATED_AT
			)
			VALUES (
				:itemType, :itemId, :embeddingText, :textHash, :modelName, :vectorBlob, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
			)
			ON DUPLICATE KEY UPDATE
				EMBEDDING_TEXT = VALUES(EMBEDDING_TEXT),
				TEXT_HASH = VALUES(TEXT_HASH),
				MODEL_NAME = VALUES(MODEL_NAME),
				VECTOR_BLOB = VALUES(VECTOR_BLOB),
				UPDATED_AT = CURRENT_TIMESTAMP
			""", nativeQuery = true)
	void upsert(
			@Param("itemType") String itemType,
			@Param("itemId") Long itemId,
			@Param("embeddingText") String embeddingText,
			@Param("textHash") String textHash,
			@Param("modelName") String modelName,
			@Param("vectorBlob") byte[] vectorBlob
	);
}
