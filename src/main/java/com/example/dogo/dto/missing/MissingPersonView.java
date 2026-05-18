package com.example.dogo.dto.missing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
		String statusLabel
) {
}
