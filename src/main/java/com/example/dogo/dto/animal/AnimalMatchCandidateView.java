package com.example.dogo.dto.animal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnimalMatchCandidateView(
		Long reportId,
		String reportType,
		String reportTypeLabel,
		String title,
		String animalType,
		String animalTypeLabel,
		String breedName,
		String furColor,
		String regionName,
		String detailPlace,
		LocalDate eventDate,
		String status,
		String statusLabel,
		BigDecimal score,
		String imageUrl
) {}
