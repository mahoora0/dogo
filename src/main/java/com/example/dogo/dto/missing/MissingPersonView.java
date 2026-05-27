package com.example.dogo.dto.missing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MissingPersonView(
		Long id,
		String summary,
		Integer age,
		String nationality,
		LocalDateTime occurredAt,
		String occurredPlace,
		Integer heightCm,
		BigDecimal weightKg,
		String bodyType,
		String faceShape,
		String hairColor,
		String hairStyle,
		String clothing,
		String status,
		String statusLabel,
		String sourceType,
		String sourceLabel,
		String base64Image,
		List<String> imageUrls
) {
}
