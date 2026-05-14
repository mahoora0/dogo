package com.example.dogo.service.match.semantic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "match.semantic.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSemanticMatchClient implements SemanticMatchClient {

	@Override
	public SemanticMatchResponse score(SemanticMatchRequest request) {
		return new SemanticMatchResponse(null, List.of());
	}
}
