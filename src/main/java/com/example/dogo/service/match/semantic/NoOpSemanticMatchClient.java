package com.example.dogo.service.match.semantic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnExpression("!${match.semantic.enabled:false} && !${match.embedding.enabled:false}")
public class NoOpSemanticMatchClient implements SemanticMatchClient {

	@Override
	public SemanticMatchResponse score(SemanticMatchRequest request) {
		return new SemanticMatchResponse(null, List.of());
	}
}
