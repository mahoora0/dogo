package com.example.dogo.service.match.semantic;

import java.util.List;

public record SemanticMatchResponse(
		String model,
		List<SemanticMatchResult> results
) {
}
