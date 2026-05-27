package com.example.dogo.dto.missing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MissingPersonCreateRequest {

	private Integer age;
	private String nationality;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime occurredAt;

	private String occurredPlace;
	private Integer heightCm;
	private BigDecimal weightKg;
	private String bodyType;
	private String faceShape;
	private String hairColor;
	private String hairStyle;
	private String clothing;
	private MultipartFile image;
	private List<MultipartFile> images = new ArrayList<>();

	public List<MultipartFile> getUploadImages() {
		List<MultipartFile> uploadImages = new ArrayList<>();
		if (image != null && !image.isEmpty()) {
			uploadImages.add(image);
		}
		if (images != null) {
			uploadImages.addAll(images.stream()
					.filter(candidate -> candidate != null && !candidate.isEmpty())
					.toList());
		}
		return uploadImages;
	}
}
