package com.example.dogo.service.missing.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Safe182MissingPersonRecord(
		String externalId,
		String name,
		String gender,
		Integer age,
		LocalDateTime occurredAt,
		String occurredPlace,
		Integer heightCm,
		BigDecimal weightKg,
		String bodyType,
		String faceShape,
		String hairColor,
		String hairStyle,
		String clothing,
		String rawPayload
) {
}
