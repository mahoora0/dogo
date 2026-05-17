package com.example.dogo.service.match.semantic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
@ConditionalOnExpression("${match.semantic.enabled:false} && !${match.embedding.enabled:false}")
public class PythonSemanticMatchClient implements SemanticMatchClient {

	private static final Logger log = LoggerFactory.getLogger(PythonSemanticMatchClient.class);

	private final RestClient restClient;

	public PythonSemanticMatchClient(SemanticMatchProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofMillis(properties.timeoutMs()));
		factory.setReadTimeout(Duration.ofMillis(properties.timeoutMs()));

		this.restClient = RestClient.builder()
				.requestFactory(factory)
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();

		log.info("Python 시맨틱 매칭 클라이언트 초기화: baseUrl={}, timeoutMs={}", properties.baseUrl(), properties.timeoutMs());
	}

	@Override
	public SemanticMatchResponse score(SemanticMatchRequest request) {
		return restClient.post()
				.uri("/similarity")
				.body(request)
				.retrieve()
				.body(SemanticMatchResponse.class);
	}
}
