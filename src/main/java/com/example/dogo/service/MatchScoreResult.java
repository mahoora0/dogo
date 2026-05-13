package com.example.dogo.service;

import java.math.BigDecimal;
import java.util.List;

public record MatchScoreResult(
		boolean eligible,
		BigDecimal totalScore,
		BigDecimal categoryScore,
		BigDecimal timeScore,
		BigDecimal locationScore,
		BigDecimal keywordScore,
		BigDecimal colorScore,
		BigDecimal detailScore,
		List<String> reasons
) {

	public static MatchScoreResult ineligible(String reason) {
		return new MatchScoreResult(
				false,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				List.of(reason)
		);
	}
}
