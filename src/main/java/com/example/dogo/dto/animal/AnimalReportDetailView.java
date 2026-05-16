package com.example.dogo.dto.animal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AnimalReportDetailView(
		Long id,
		String reportType,
		String reportTypeLabel,
		String status,
		String statusLabel,
		String title,
		LocalDate eventDate,
		LocalTime eventTime,
		String regionName,
		String detailPlace,
		String contactPhone,
		boolean contactPublic,
		String displayContact,
		String sightingCareStatus,
		String sightingCareStatusLabel,
		String animalType,
		String animalTypeLabel,
		String breedName,
		String gender,
		String genderLabel,
		String neuteredStatus,
		String neuteredStatusLabel,
		Integer ageValue,
		String ageUnit,
		String ageUnitLabel,
		BigDecimal weightKg,
		String furColor,
		String distinctiveMarks,
		String content,
		int viewCount,
		List<String> imageUrls
) {
}
