package com.example.dogo.dto.animal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AnimalReportEditData(
		Long id,
		String reportType,
		String title,
		LocalDate eventDate,
		LocalTime eventTime,
		String regionName,
		String detailPlace,
		String contactPhone,
		boolean contactPublic,
		String sightingCareStatus,
		String animalType,
		String breedName,
		String gender,
		String neuteredStatus,
		Integer ageValue,
		String ageUnit,
		BigDecimal weightKg,
		String furColor,
		String distinctiveMarks,
		String content,
		List<String> existingImageUrls
) {}
