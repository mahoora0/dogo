package com.example.dogo.service.match.semantic;

public interface SemanticMatchClient {
	SemanticMatchResponse score(SemanticMatchRequest request);
}
