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
	private static final String ANIMAL_CROP = "ANIMAL_CROP";
	private static final String ORIGINAL_FALLBACK = "ORIGINAL_FALLBACK";

	public record ImageSearchHit(Long reportId, float score) {}
	public enum EmbedStatus { SAVED, SKIPPED, FAILED }

	private final AnimalReportImageRepository imageRepository;
	private final AnimalReportImageEmbeddingRepository embeddingRepository;
	private final PetImageEmbeddingClient embeddingClient;
	private final Path uploadDir;
	private final String currentModelName;
	private final String currentCropType;

	public AnimalImageEmbeddingService(
			AnimalReportImageRepository imageRepository,
			AnimalReportImageEmbeddingRepository embeddingRepository,
			PetImageEmbeddingClient embeddingClient,
			@Value("${file.upload-dir}") String uploadDir,
			@Value("${match.pet.embedding.model-name:AvitoTech/CLIP-ViT-base-for-animal-identification}") String currentModelName,
			@Value("${match.pet.embedding.crop-type:ANIMAL_CROP}") String currentCropType
	) {
		this.imageRepository = imageRepository;
		this.embeddingRepository = embeddingRepository;
		this.embeddingClient = embeddingClient;
		this.uploadDir = Paths.get(uploadDir);
		this.currentModelName = currentModelName;
		this.currentCropType = currentCropType;
	}

	@Transactional
	public EmbedStatus embedAndSave(AnimalReport report) {
		Optional<AnimalReportImage> firstImage =
				imageRepository.findFirstByAnimalReportOrderBySortOrderAscImageIdAsc(report);

		if (firstImage.isEmpty()) {
			log.debug("[pet-embedding] 이미지 없음, 스킵: reportId={}", report.getReportId());
			return EmbedStatus.SKIPPED;
		}

		AnimalReportImage image = firstImage.get();
		byte[] imageBytes = readImageBytes(image.getStoredName());
		if (imageBytes == null) return EmbedStatus.FAILED;

		Optional<AnimalReportImageEmbedding> existing = embeddingRepository.findByReportReportId(report.getReportId());
		if (existing.isPresent()
				&& currentModelName.equals(existing.get().getModelName())
				&& acceptedCropTypes().contains(existing.get().getCropType())) {
			log.debug("[pet-embedding] 현재 모델/crop 벡터 존재, 스킵: reportId={}, model={}, cropType={}",
					report.getReportId(), currentModelName, existing.get().getCropType());
			return EmbedStatus.SKIPPED;
		}

		PetImageEmbeddingClient.EmbeddingResult result =
				embeddingClient.embed(imageBytes, image.getStoredName(), report.getAnimalType());
		if (!result.hasVector()) {
			log.warn("[pet-embedding] 빈 벡터 반환, 스킵: reportId={}", report.getReportId());
			return EmbedStatus.FAILED;
		}

		byte[] blob = VectorUtils.toBytes(result.vector());
		String modelName = normalizeModelName(result.modelName());
		String cropType = normalizeCropType(result.cropType());

		existing.ifPresentOrElse(
				saved -> saved.update(image, blob, modelName, cropType),
				() -> embeddingRepository.save(new AnimalReportImageEmbedding(report, image, blob, modelName, cropType))
		);
		log.info("[pet-embedding] 저장 완료: reportId={}, model={}, cropType={}",
				report.getReportId(), modelName, cropType);
		return EmbedStatus.SAVED;
	}

	@Transactional(readOnly = true)
	public List<ImageSearchHit> searchByImage(byte[] imageBytes, String filename) {
		PetImageEmbeddingClient.EmbeddingResult result = embeddingClient.embed(imageBytes, filename);
		if (!result.hasVector()) {
			log.warn("[pet-image-search] 빈 쿼리 벡터 반환");
			return List.of();
		}
		String modelName = normalizeModelName(result.modelName());
		String cropType = normalizeCropType(result.cropType());
		return embeddingRepository.findByModelNameAndCropType(modelName, cropType).stream()
				.map(e -> new ImageSearchHit(
						e.getReport().getReportId(),
						VectorUtils.cosineSimilarity(result.vector(), VectorUtils.fromBytes(e.getVectorBlob()))
				))
				.filter(h -> h.score() >= IMAGE_SEARCH_THRESHOLD)
				.sorted(Comparator.comparingDouble(ImageSearchHit::score).reversed())
				.limit(IMAGE_SEARCH_MAX_RESULTS)
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<Long, float[]> loadVectors(List<Long> reportIds) {
		if (reportIds.isEmpty()) return Map.of();
		Map<Long, float[]> result = new HashMap<>();
		embeddingRepository.findByReportIdsAndModelNameAndCropTypes(reportIds, currentModelName, acceptedCropTypes())
				.forEach(e -> result.put(e.getReport().getReportId(), VectorUtils.fromBytes(e.getVectorBlob())));
		return result;
	}

	public String currentModelName() {
		return currentModelName;
	}

	public String currentCropType() {
		return currentCropType;
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

	private String normalizeModelName(String modelName) {
		if (modelName == null || modelName.isBlank()) {
			return currentModelName;
		}
		return modelName;
	}

	private String normalizeCropType(String cropType) {
		if (cropType == null || cropType.isBlank()) {
			return currentCropType;
		}
		return cropType;
	}

	private List<String> acceptedCropTypes() {
		if (ANIMAL_CROP.equals(currentCropType)) {
			return List.of(ANIMAL_CROP, ORIGINAL_FALLBACK);
		}
		return List.of(currentCropType);
	}
}
