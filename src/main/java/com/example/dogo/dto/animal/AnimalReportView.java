package com.example.dogo.dto.animal;

import java.time.LocalDate;
import java.time.LocalTime;

public record AnimalReportView(
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
		String animalType,
		String animalTypeLabel,
		String breedName,
		String furColor,
		String sightingCareStatus,
		String sightingCareStatusLabel,
		String imageUrl
) {
}
