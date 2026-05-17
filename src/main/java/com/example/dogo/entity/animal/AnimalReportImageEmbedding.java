package com.example.dogo.entity.animal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ANIMAL_REPORT_IMAGE_EMBEDDING")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalReportImageEmbedding {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "EMBEDDING_ID")
	private Long embeddingId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "REPORT_ID", nullable = false, unique = true)
	private AnimalReport report;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "IMAGE_ID", nullable = false)
	private AnimalReportImage image;

	@Column(name = "VECTOR_BLOB", nullable = false)
	private byte[] vectorBlob;

	@Column(name = "MODEL_NAME", nullable = false)
	private String modelName;

	@Column(name = "CREATED_AT", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();

	public AnimalReportImageEmbedding(AnimalReport report, AnimalReportImage image, byte[] vectorBlob, String modelName) {
		this.report = report;
		this.image = image;
		this.vectorBlob = vectorBlob;
		this.modelName = modelName;
	}

	public void update(AnimalReportImage image, byte[] vectorBlob, String modelName) {
		this.image = image;
		this.vectorBlob = vectorBlob;
		this.modelName = modelName;
		this.updatedAt = LocalDateTime.now();
	}
}
