package com.example.dogo.service.match.embedding;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.ItemEmbedding;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.repository.item.ItemEmbeddingRepository;
import com.example.dogo.service.match.semantic.SemanticMatchItem;
import com.example.dogo.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "match.embedding.enabled", havingValue = "true")
public class ItemEmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(ItemEmbeddingService.class);

	private final ItemEmbeddingRepository embeddingRepository;
	private final PythonEmbeddingClient embeddingClient;

	public ItemEmbeddingService(ItemEmbeddingRepository embeddingRepository,
			PythonEmbeddingClient embeddingClient) {
		this.embeddingRepository = embeddingRepository;
		this.embeddingClient = embeddingClient;
	}

	@Transactional
	public void embedAndSave(LostItem item) {
		String text = buildText(item.getItemName(), item.getTitle());
		if (!StringUtils.hasText(text)) return;
		embedAndSave("LOST", item.getLostId(), text);
	}

	@Transactional
	public void embedAndSave(FoundItem item) {
		String text = buildText(item.getItemName(), item.getTitle());
		if (!StringUtils.hasText(text)) return;
		embedAndSave("FOUND", item.getFoundId(), text);
	}

	/** 분실물 배치 임베딩 — 이미 임베딩된 항목은 스킵 */
	@Transactional
	public int embedLostBatch(List<LostItem> items) {
		List<EmbeddingItem> batch = items.stream()
				.map(item -> new EmbeddingItem(item.getLostId(), buildText(item.getItemName(), item.getTitle())))
				.toList();
		return embedBatch("LOST", batch);
	}

	/** 습득물 배치 임베딩 — 이미 임베딩된 항목은 스킵 */
	@Transactional
	public int embedFoundBatch(List<FoundItem> items) {
		List<EmbeddingItem> batch = items.stream()
				.map(item -> new EmbeddingItem(item.getFoundId(), buildText(item.getItemName(), item.getTitle())))
				.toList();
		return embedBatch("FOUND", batch);
	}

	/** DB에서 조회하고, 없는 것은 Python 호출 후 저장해서 전부 반환 */
	@Transactional
	public Map<Long, float[]> getOrFetch(String itemType, List<SemanticMatchItem> candidates) {
		if (candidates.isEmpty()) return Map.of();

		List<Long> ids = candidates.stream().map(SemanticMatchItem::id).toList();
		Map<Long, float[]> result = new HashMap<>();

		embeddingRepository.findByItemTypeAndItemIdIn(itemType, ids)
				.forEach(e -> result.put(e.getItemId(), VectorUtils.fromBytes(e.getVectorBlob())));

		List<EmbeddingItem> missing = candidates.stream()
				.filter(c -> !result.containsKey(c.id()))
				.map(c -> new EmbeddingItem(c.id(), buildText(c.itemName(), c.title())))
				.filter(e -> StringUtils.hasText(e.text()))
				.toList();

		if (!missing.isEmpty()) {
			log.info("[embedding] DB 미보유 {}건 Python 실시간 요청 (itemType={})", missing.size(), itemType);
			Map<Long, float[]> fetched = embeddingClient.embedItems(missing);
			fetched.forEach((id, vector) -> {
				EmbeddingItem req = missing.stream().filter(e -> e.id() == id).findFirst().orElseThrow();
				String hash = sha256(req.text());
				embeddingRepository.save(
						new ItemEmbedding(itemType, id, req.text(), hash, "", VectorUtils.toBytes(vector)));
				result.put(id, vector);
			});
		}

		return result;
	}

	private int embedBatch(String itemType, List<EmbeddingItem> items) {
		List<EmbeddingItem> candidates = items.stream()
				.filter(e -> StringUtils.hasText(e.text()))
				.toList();
		if (candidates.isEmpty()) return 0;

		Set<Long> existing = embeddingRepository.findByItemTypeAndItemIdIn(itemType,
				candidates.stream().map(EmbeddingItem::id).toList())
				.stream().map(ItemEmbedding::getItemId).collect(Collectors.toSet());

		List<EmbeddingItem> missing = candidates.stream()
				.filter(e -> !existing.contains(e.id()))
				.toList();
		if (missing.isEmpty()) return 0;

		Map<Long, float[]> vectors = embeddingClient.embedItems(missing);
		Map<Long, String> textById = missing.stream().collect(Collectors.toMap(EmbeddingItem::id, EmbeddingItem::text));
		vectors.forEach((id, vector) -> {
			String text = textById.get(id);
			embeddingRepository.save(
					new ItemEmbedding(itemType, id, text, sha256(text), "", VectorUtils.toBytes(vector)));
		});
		return vectors.size();
	}

	private void embedAndSave(String itemType, Long itemId, String text) {
		String hash = sha256(text);
		Optional<ItemEmbedding> existing = embeddingRepository.findByItemTypeAndItemId(itemType, itemId);

		if (existing.isPresent() && existing.get().getTextHash().equals(hash)) {
			log.debug("[embedding] 변경 없음, 스킵: itemType={} itemId={}", itemType, itemId);
			return;
		}

		log.info("[embedding] 임베딩 요청: itemType={} itemId={} text={}", itemType, itemId, text);
		float[] vector = embeddingClient.embedText(text);
		if (vector.length == 0) {
			log.warn("[embedding] 빈 벡터 반환, 저장 스킵: itemType={} itemId={}", itemType, itemId);
			return;
		}

		byte[] blob = VectorUtils.toBytes(vector);
		if (existing.isPresent()) {
			existing.get().update(text, hash, "", blob);
		} else {
			embeddingRepository.save(new ItemEmbedding(itemType, itemId, text, hash, "", blob));
		}
		log.info("[embedding] 저장 완료: itemType={} itemId={}", itemType, itemId);
	}

	private String buildText(String itemName, String title) {
		if (StringUtils.hasText(itemName)) return itemName.trim();
		if (StringUtils.hasText(title)) return title.trim();
		return "";
	}

	private String sha256(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(64);
			for (byte b : hash) sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
