package com.example.dogo.entity.missing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MISSING_PERSON_IMAGE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissingPersonImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "IMAGE_ID")
	private Long imageId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "REPORT_ID", nullable = false)
	private MissingPersonReport report;

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

	public MissingPersonImage(
			MissingPersonReport report,
			String originalName,
			String storedName,
			String imageUrl,
			String contentType,
			Long fileSize,
			int sortOrder
	) {
		this.report = report;
		this.originalName = originalName;
		this.storedName = storedName;
		this.imageUrl = imageUrl;
		this.contentType = contentType;
		this.fileSize = fileSize;
		this.sortOrder = sortOrder;
	}
}
