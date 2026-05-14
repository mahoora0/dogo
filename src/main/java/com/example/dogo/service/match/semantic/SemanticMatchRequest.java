package com.example.dogo.service.match.semantic;

import java.util.List;

public record SemanticMatchRequest(
		SemanticMatchItem query,
		List<SemanticMatchItem> candidates
) {
}
