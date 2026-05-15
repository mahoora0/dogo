package com.example.dogo.service.match.semantic;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "match.semantic")
public record SemanticMatchProperties(
		boolean enabled,
		String baseUrl,
		int timeoutMs
) {
}
