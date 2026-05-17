package com.example.dogo.dto.match;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MatchCandidateView(
		Long id,
		String type,
		String title,
		String name,
		String category,
		String area,
		String place,
		LocalDateTime eventAt,
		String status,
		String statusLabel,
		String imageUrl,
		BigDecimal score,
		List<String> matchReasons
) {
}
