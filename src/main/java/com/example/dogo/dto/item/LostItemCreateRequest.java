package com.example.dogo.dto.item;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LostItemCreateRequest {

	private String title;
	private String itemName;
	private String categoryMain;
	private String categorySub;
	private String colorName;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime lostAt;

	private String lostAreaProvince;
	private String lostAreaDistrict;
	private String lostArea;
	private String lostPlace;
	private String contact;
	private String content;
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
