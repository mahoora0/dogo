package com.example.dogo.entity.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ITEM_EMBEDDING")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemEmbedding {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "EMBEDDING_ID")
	private Long embeddingId;

	@Column(name = "ITEM_TYPE", nullable = false, length = 10)
	private String itemType;

	@Column(name = "ITEM_ID", nullable = false)
	private Long itemId;

	@Column(name = "EMBEDDING_TEXT", nullable = false, length = 1000)
	private String embeddingText;

	@Column(name = "TEXT_HASH", nullable = false, length = 64)
	private String textHash;

	@Column(name = "MODEL_NAME", nullable = false, length = 200)
	private String modelName;

	@Lob
	@Column(name = "VECTOR_BLOB", nullable = false, columnDefinition = "LONGBLOB")
	private byte[] vectorBlob;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;

	public ItemEmbedding(String itemType, Long itemId, String embeddingText, String textHash,
			String modelName, byte[] vectorBlob) {
		this.itemType = itemType;
		this.itemId = itemId;
		this.embeddingText = embeddingText;
		this.textHash = textHash;
		this.modelName = modelName;
		this.vectorBlob = vectorBlob;
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	public void update(String embeddingText, String textHash, String modelName, byte[] vectorBlob) {
		this.embeddingText = embeddingText;
		this.textHash = textHash;
		this.modelName = modelName;
		this.vectorBlob = vectorBlob;
		this.updatedAt = LocalDateTime.now();
	}
}
