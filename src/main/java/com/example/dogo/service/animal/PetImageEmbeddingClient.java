package com.example.dogo.service.animal;

import com.example.dogo.service.match.semantic.SemanticMatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "match.pet.embedding.enabled", havingValue = "true")
public class PetImageEmbeddingClient {

	private final RestClient restClient;

	public PetImageEmbeddingClient(SemanticMatchProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofMillis(properties.timeoutMs()));
		factory.setReadTimeout(Duration.ofMillis(properties.timeoutMs() * 3L));

		this.restClient = RestClient.builder()
				.requestFactory(factory)
				.baseUrl(properties.baseUrl())
				.build();
	}

	public float[] embed(byte[] imageBytes, String filename) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("image", new ByteArrayResource(imageBytes) {
			@Override
			public String getFilename() {
				return filename;
			}
		});

		PetImageEmbeddingResponse response = restClient.post()
				.uri("/pet-image-embedding")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(PetImageEmbeddingResponse.class);

		if (response == null || response.vector() == null || response.vector().isEmpty()) {
			return new float[0];
		}
		return toFloatArray(response.vector());
	}

	public String modelName() {
		return "openai/clip-vit-base-patch32";
	}

	private float[] toFloatArray(List<Float> list) {
		float[] arr = new float[list.size()];
		for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
		return arr;
	}

	record PetImageEmbeddingResponse(List<Float> vector, String model) {}
}
