package com.example.dogo.service.match.embedding;

import com.example.dogo.service.match.semantic.SemanticMatchProperties;
import com.example.dogo.util.VectorUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "match.embedding.enabled", havingValue = "true")
public class PythonEmbeddingClient {

	private final RestClient restClient;

	public PythonEmbeddingClient(SemanticMatchProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofMillis(properties.timeoutMs()));
		factory.setReadTimeout(Duration.ofMillis(properties.timeoutMs()));

		this.restClient = RestClient.builder()
				.requestFactory(factory)
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	public float[] embedText(String text) {
		EmbeddingsResponse response = post(List.of(new EmbeddingItem(0L, text)));
		if (response.embeddings().isEmpty()) return new float[0];
		return toFloatArray(response.embeddings().get(0).vector());
	}

	public Map<Long, float[]> embedItems(List<EmbeddingItem> items) {
		if (items.isEmpty()) return Map.of();
		EmbeddingsResponse response = post(items);
		return response.embeddings().stream()
				.collect(Collectors.toMap(EmbeddingVector::id, ev -> toFloatArray(ev.vector())));
	}

	public String getModelName(String text) {
		EmbeddingsResponse response = post(List.of(new EmbeddingItem(0L, text)));
		return response.model();
	}

	private EmbeddingsResponse post(List<EmbeddingItem> items) {
		return restClient.post()
				.uri("/embeddings")
				.body(new EmbeddingsRequest(items))
				.retrieve()
				.body(EmbeddingsResponse.class);
	}

	private float[] toFloatArray(List<Float> list) {
		float[] arr = new float[list.size()];
		for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
		return arr;
	}
}
