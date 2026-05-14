package com.example.dogo.service.match.semantic;

import java.math.BigDecimal;
import java.util.List;

public record SemanticMatchResult(
		long candidateId,
		BigDecimal semanticScore,
		List<String> reasons
) {
}
