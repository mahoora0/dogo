package com.example.dogo.dto.animal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AnimalReportCreateRequest {

	private String reportType;
	private String title;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate eventDate;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime eventTime;

	private String regionName;
	private String detailPlace;
	private String contactPhone;
	private boolean contactPublic = true;
	private String sightingCareStatus;
	private String careLocationName;
	private String careLocationAddress;
	private String careContactPhone;
	private String animalType;
	private String breedName;
	private String gender = "UNKNOWN";
	private String neuteredStatus = "UNKNOWN";
	private Integer ageValue;
	private String ageUnit;
	private BigDecimal weightKg;
	private String furColor;
	private String distinctiveMarks;
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
