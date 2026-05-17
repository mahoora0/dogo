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

	public EmbeddingResult embed(byte[] imageBytes, String filename) {
		return embed(imageBytes, filename, null);
	}

	public EmbeddingResult embed(byte[] imageBytes, String filename, String animalType) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("image", new ByteArrayResource(imageBytes) {
			@Override
			public String getFilename() {
				return filename;
			}
		});
		if (animalType != null && !animalType.isBlank()) {
			body.add("animalType", animalType);
		}

		PetImageEmbeddingResponse response = restClient.post()
				.uri("/pet-image-embedding")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(PetImageEmbeddingResponse.class);

		if (response == null || response.vector() == null || response.vector().isEmpty()) {
			return new EmbeddingResult(new float[0],
					response == null ? null : response.model(),
					response == null ? null : response.cropType());
		}
		return new EmbeddingResult(toFloatArray(response.vector()), response.model(), response.cropType());
	}

	private float[] toFloatArray(List<Float> list) {
		float[] arr = new float[list.size()];
		for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
		return arr;
	}

	public record EmbeddingResult(float[] vector, String modelName, String cropType) {
		public boolean hasVector() {
			return vector != null && vector.length > 0;
		}
	}

	record PetImageEmbeddingResponse(List<Float> vector, String model, String cropType) {}
}
