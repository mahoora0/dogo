package com.example.dogo.service.animal;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.animal.AnimalReportImage;
import com.example.dogo.entity.animal.AnimalReportImageEmbedding;
import com.example.dogo.repository.animal.AnimalReportImageEmbeddingRepository;
import com.example.dogo.repository.animal.AnimalReportImageRepository;
import com.example.dogo.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "match.pet.embedding.enabled", havingValue = "true")
public class AnimalImageEmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(AnimalImageEmbeddingService.class);
	private static final String ANIMAL_UPLOAD_SUBDIR = "animal-reports";
	private static final float IMAGE_SEARCH_THRESHOLD = 0.5f;
	private static final int IMAGE_SEARCH_MAX_RESULTS = 20;

	public record ImageSearchHit(Long reportId, float score) {}

	private final AnimalReportImageRepository imageRepository;
	private final AnimalReportImageEmbeddingRepository embeddingRepository;
	private final PetImageEmbeddingClient embeddingClient;
	private final Path uploadDir;

	public AnimalImageEmbeddingService(
			AnimalReportImageRepository imageRepository,
			AnimalReportImageEmbeddingRepository embeddingRepository,
			PetImageEmbeddingClient embeddingClient,
			@Value("${file.upload-dir}") String uploadDir
	) {
		this.imageRepository = imageRepository;
		this.embeddingRepository = embeddingRepository;
		this.embeddingClient = embeddingClient;
		this.uploadDir = Paths.get(uploadDir);
	}

	@Transactional
	public void embedAndSave(AnimalReport report) {
		Optional<AnimalReportImage> firstImage =
				imageRepository.findFirstByAnimalReportOrderBySortOrderAscImageIdAsc(report);

		if (firstImage.isEmpty()) {
			log.debug("[pet-embedding] 이미지 없음, 스킵: reportId={}", report.getReportId());
			return;
		}

		AnimalReportImage image = firstImage.get();
		byte[] imageBytes = readImageBytes(image.getStoredName());
		if (imageBytes == null) return;

		float[] vector = embeddingClient.embed(imageBytes, image.getStoredName());
		if (vector.length == 0) {
			log.warn("[pet-embedding] 빈 벡터 반환, 스킵: reportId={}", report.getReportId());
			return;
		}

		byte[] blob = VectorUtils.toBytes(vector);
		String modelName = embeddingClient.modelName();

		embeddingRepository.findByReportReportId(report.getReportId()).ifPresentOrElse(
				existing -> existing.update(image, blob, modelName),
				() -> embeddingRepository.save(new AnimalReportImageEmbedding(report, image, blob, modelName))
		);
		log.info("[pet-embedding] 저장 완료: reportId={}", report.getReportId());
	}

	@Transactional(readOnly = true)
	public List<ImageSearchHit> searchByImage(byte[] imageBytes, String filename) {
		float[] queryVector = embeddingClient.embed(imageBytes, filename);
		if (queryVector.length == 0) {
			log.warn("[pet-image-search] 빈 쿼리 벡터 반환");
			return List.of();
		}
		return embeddingRepository.findAll().stream()
				.map(e -> new ImageSearchHit(
						e.getReport().getReportId(),
						VectorUtils.cosineSimilarity(queryVector, VectorUtils.fromBytes(e.getVectorBlob()))
				))
				.filter(h -> h.score() >= IMAGE_SEARCH_THRESHOLD)
				.sorted(Comparator.comparingDouble(ImageSearchHit::score).reversed())
				.limit(IMAGE_SEARCH_MAX_RESULTS)
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<Long, float[]> loadVectors(List<Long> reportIds) {
		Map<Long, float[]> result = new HashMap<>();
		embeddingRepository.findByReportIds(reportIds)
				.forEach(e -> result.put(e.getReport().getReportId(), VectorUtils.fromBytes(e.getVectorBlob())));
		return result;
	}

	private byte[] readImageBytes(String storedName) {
		Path imagePath = uploadDir.resolve(ANIMAL_UPLOAD_SUBDIR).resolve(storedName).normalize();
		if (!imagePath.startsWith(uploadDir)) {
			log.warn("[pet-embedding] 경로 탐색 감지, 스킵: {}", storedName);
			return null;
		}
		try {
			return Files.readAllBytes(imagePath);
		} catch (IOException e) {
			log.warn("[pet-embedding] 이미지 파일 읽기 실패: {} - {}", storedName, e.getMessage());
			return null;
		}
	}
}
