package com.example.dogo.service.animal;

import com.example.dogo.service.match.semantic.SemanticMatchProperties;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "match.pet.embedding.enabled", havingValue = "true")
public class PetImageEmbeddingClient {

	private final RestClient restClient;

	public PetImageEmbeddingClient(
			SemanticMatchProperties properties,
			@Value("${match.pet.embedding.timeout-ms:60000}") int timeoutMs
	) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofMillis(properties.timeoutMs()));
		factory.setReadTimeout(Duration.ofMillis(timeoutMs));

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

	public Map<Long, EmbeddingResult> embedBatch(List<BatchEmbeddingRequest> requests) {
		if (requests.isEmpty()) return Map.of();

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		for (BatchEmbeddingRequest request : requests) {
			body.add("ids", String.valueOf(request.id()));
			body.add("animalTypes", request.animalType() == null ? "" : request.animalType());
			body.add("images", new ByteArrayResource(request.imageBytes()) {
				@Override
				public String getFilename() {
					return request.filename();
				}
			});
		}

		PetImageEmbeddingsResponse response = restClient.post()
				.uri("/pet-image-embeddings")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(PetImageEmbeddingsResponse.class);

		if (response == null || response.embeddings() == null) {
			return Map.of();
		}
		return response.embeddings().stream()
				.collect(Collectors.toMap(
						PetImageEmbeddingItem::id,
						item -> new EmbeddingResult(toFloatArray(item.vector()), item.model(), item.cropType())
				));
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

	public record BatchEmbeddingRequest(long id, byte[] imageBytes, String filename, String animalType) {}

	record PetImageEmbeddingResponse(List<Float> vector, String model, String cropType) {}

	record PetImageEmbeddingItem(Long id, List<Float> vector, String model, String cropType) {}

	record PetImageEmbeddingsResponse(List<PetImageEmbeddingItem> embeddings) {}
}
