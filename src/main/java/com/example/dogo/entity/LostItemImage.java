package com.example.dogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "LOST_ITEM_IMAGE")
public class LostItemImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "IMAGE_ID")
	private Long imageId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "LOST_ID", nullable = false)
	private LostItem lostItem;

	@Column(name = "ORIGINAL_NAME")
	private String originalName;

	@Column(name = "STORED_NAME", nullable = false)
	private String storedName;

	@Column(name = "IMAGE_URL", nullable = false)
	private String imageUrl;

	@Column(name = "CONTENT_TYPE")
	private String contentType;

	@Column(name = "FILE_SIZE")
	private Long fileSize;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;

	protected LostItemImage() {
	}

	public LostItemImage(
			LostItem lostItem,
			String originalName,
			String storedName,
			String imageUrl,
			String contentType,
			Long fileSize,
			int sortOrder
	) {
		this.lostItem = lostItem;
		this.originalName = originalName;
		this.storedName = storedName;
		this.imageUrl = imageUrl;
		this.contentType = contentType;
		this.fileSize = fileSize;
		this.sortOrder = sortOrder;
	}

	public String getImageUrl() {
		return imageUrl;
	}
}
