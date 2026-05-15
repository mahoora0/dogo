package com.example.dogo.service.match.embedding;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.ItemEmbedding;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.repository.item.ItemEmbeddingRepository;
import com.example.dogo.service.match.semantic.SemanticMatchItem;
import com.example.dogo.service.match.semantic.SemanticMatchTextBuilder;
import com.example.dogo.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "match.embedding.enabled", havingValue = "true")
public class ItemEmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(ItemEmbeddingService.class);

	private final ItemEmbeddingRepository embeddingRepository;
	private final PythonEmbeddingClient embeddingClient;
	private final SemanticMatchTextBuilder textBuilder;

	public ItemEmbeddingService(ItemEmbeddingRepository embeddingRepository,
			PythonEmbeddingClient embeddingClient,
			SemanticMatchTextBuilder textBuilder) {
		this.embeddingRepository = embeddingRepository;
		this.embeddingClient = embeddingClient;
		this.textBuilder = textBuilder;
	}

	@Transactional
	public void embedAndSave(LostItem item) {
		String text = textBuilder.build(item);
		if (!StringUtils.hasText(text)) return;
		embedAndSave("LOST", item.getLostId(), text);
	}

	@Transactional
	public void embedAndSave(FoundItem item) {
		String text = textBuilder.build(item);
		if (!StringUtils.hasText(text)) return;
		embedAndSave("FOUND", item.getFoundId(), text);
	}

	/** 분실물 배치 임베딩 — 이미 임베딩된 항목은 스킵 */
	@Transactional
	public int embedLostBatch(List<LostItem> items) {
		List<EmbeddingItem> batch = items.stream()
				.map(item -> new EmbeddingItem(item.getLostId(), textBuilder.build(item)))
				.toList();
		return embedBatch("LOST", batch);
	}

	/** 습득물 배치 임베딩 — 이미 임베딩된 항목은 스킵 */
	@Transactional
	public int embedFoundBatch(List<FoundItem> items) {
		List<EmbeddingItem> batch = items.stream()
				.map(item -> new EmbeddingItem(item.getFoundId(), textBuilder.build(item)))
				.toList();
		return embedBatch("FOUND", batch);
	}

	/** DB에서 조회하고, 없는 것은 Python 호출 후 저장해서 전부 반환 */
	@Transactional
	public Map<Long, float[]> getOrFetch(String itemType, List<SemanticMatchItem> candidates) {
		if (candidates.isEmpty()) return Map.of();

		List<EmbeddingItem> requested = candidates.stream()
				.map(c -> new EmbeddingItem(c.id(), textBuilder.build(c)))
				.filter(e -> StringUtils.hasText(e.text()))
				.toList();
		if (requested.isEmpty()) return Map.of();

		List<Long> ids = requested.stream().map(EmbeddingItem::id).toList();
		Map<Long, EmbeddingItem> requestedById = requested.stream()
				.collect(Collectors.toMap(EmbeddingItem::id, e -> e, (left, right) -> left));
		Map<Long, float[]> result = new HashMap<>();
		Map<Long, ItemEmbedding> existingById = embeddingRepository.findByItemTypeAndItemIdIn(itemType, ids)
				.stream()
				.collect(Collectors.toMap(ItemEmbedding::getItemId, e -> e, (left, right) -> left));

		List<EmbeddingItem> missing = requested.stream()
				.filter(e -> {
					ItemEmbedding existing = existingById.get(e.id());
					if (existing == null) {
						return true;
					}
					String expectedHash = textBuilder.hash(e.text());
					if (!expectedHash.equals(existing.getTextHash())) {
						return true;
					}
					result.put(e.id(), VectorUtils.fromBytes(existing.getVectorBlob()));
					return false;
				})
				.toList();

		if (!missing.isEmpty()) {
			log.info("[embedding] DB 미보유 {}건 Python 실시간 요청 (itemType={})", missing.size(), itemType);
			Map<Long, float[]> fetched = embeddingClient.embedItems(missing);
			fetched.forEach((id, vector) -> {
				EmbeddingItem req = requestedById.get(id);
				if (req == null) {
					return;
				}
				saveOrUpdate(itemType, id, req.text(), vector, existingById.get(id));
				result.put(id, vector);
			});
		}

		return result;
	}

	@Transactional
	public float[] getOrFetchOne(String itemType, SemanticMatchItem item) {
		String text = textBuilder.build(item);
		if (!StringUtils.hasText(text)) {
			return new float[0];
		}

		Optional<ItemEmbedding> existing = embeddingRepository.findByItemTypeAndItemId(itemType, item.id());
		String hash = textBuilder.hash(text);
		if (existing.isPresent() && existing.get().getTextHash().equals(hash)) {
			return VectorUtils.fromBytes(existing.get().getVectorBlob());
		}

		log.info("[embedding] 단건 Python 실시간 요청: itemType={} itemId={}", itemType, item.id());
		float[] vector = embeddingClient.embedText(text);
		if (vector.length == 0) {
			log.warn("[embedding] 빈 벡터 반환, 저장 스킵: itemType={} itemId={}", itemType, item.id());
			return new float[0];
		}

		saveOrUpdate(itemType, item.id(), text, vector, existing.orElse(null));
		return vector;
	}

	private int embedBatch(String itemType, List<EmbeddingItem> items) {
		List<EmbeddingItem> candidates = items.stream()
				.filter(e -> StringUtils.hasText(e.text()))
				.toList();
		if (candidates.isEmpty()) return 0;

		Map<Long, ItemEmbedding> existingById = embeddingRepository.findByItemTypeAndItemIdIn(itemType,
				candidates.stream().map(EmbeddingItem::id).toList())
				.stream()
				.collect(Collectors.toMap(ItemEmbedding::getItemId, e -> e, (left, right) -> left));

		List<EmbeddingItem> missing = candidates.stream()
				.filter(e -> {
					ItemEmbedding existing = existingById.get(e.id());
					return existing == null || !textBuilder.hash(e.text()).equals(existing.getTextHash());
				})
				.toList();
		if (missing.isEmpty()) return 0;

		Map<Long, float[]> vectors = embeddingClient.embedItems(missing);
		Map<Long, String> textById = missing.stream().collect(Collectors.toMap(EmbeddingItem::id, EmbeddingItem::text));
		vectors.forEach((id, vector) -> {
			String text = textById.get(id);
			if (text != null) {
				saveOrUpdate(itemType, id, text, vector, existingById.get(id));
			}
		});
		return vectors.size();
	}

	private void embedAndSave(String itemType, Long itemId, String text) {
		String hash = textBuilder.hash(text);
		Optional<ItemEmbedding> existing = embeddingRepository.findByItemTypeAndItemId(itemType, itemId);

		if (existing.isPresent() && existing.get().getTextHash().equals(hash)) {
			log.debug("[embedding] 변경 없음, 스킵: itemType={} itemId={}", itemType, itemId);
			return;
		}

		float[] vector = embeddingClient.embedText(text);
		if (vector.length == 0) {
			log.warn("[embedding] 빈 벡터 반환, 저장 스킵: itemType={} itemId={}", itemType, itemId);
			return;
		}

		byte[] blob = VectorUtils.toBytes(vector);
		if (existing.isPresent()) {
			existing.get().update(text, hash, SemanticMatchTextBuilder.TEXT_VERSION, blob);
		} else {
			embeddingRepository.save(new ItemEmbedding(
					itemType, itemId, text, hash, SemanticMatchTextBuilder.TEXT_VERSION, blob));
		}
	}

	private void saveOrUpdate(String itemType, Long itemId, String text, float[] vector, ItemEmbedding existing) {
		String hash = textBuilder.hash(text);
		byte[] blob = VectorUtils.toBytes(vector);
		if (existing != null) {
			existing.update(text, hash, SemanticMatchTextBuilder.TEXT_VERSION, blob);
		} else {
			embeddingRepository.save(new ItemEmbedding(
					itemType, itemId, text, hash, SemanticMatchTextBuilder.TEXT_VERSION, blob));
		}
	}
}
