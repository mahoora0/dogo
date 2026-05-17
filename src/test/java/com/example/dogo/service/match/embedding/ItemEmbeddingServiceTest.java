package com.example.dogo.service.match.embedding;

import com.example.dogo.entity.item.ItemEmbedding;
import com.example.dogo.repository.item.ItemEmbeddingRepository;
import com.example.dogo.service.match.semantic.SemanticMatchItem;
import com.example.dogo.service.match.semantic.SemanticMatchTextBuilder;
import com.example.dogo.util.VectorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemEmbeddingServiceTest {

	@Mock
	private ItemEmbeddingRepository embeddingRepository;

	@Mock
	private PythonEmbeddingClient embeddingClient;

	private SemanticMatchTextBuilder textBuilder;
	private ItemEmbeddingService service;

	@BeforeEach
	void setUp() {
		textBuilder = new SemanticMatchTextBuilder();
		service = new ItemEmbeddingService(embeddingRepository, embeddingClient, textBuilder);
	}

	@Test
	void getOrFetchUsesCachedVectorWhenVersionedHashMatches() {
		SemanticMatchItem item = item(10L, "검정 카드지갑");
		String text = textBuilder.build(item);
		float[] cachedVector = new float[] {0.1f, 0.2f, 0.3f};
		ItemEmbedding existing = new ItemEmbedding(
				"FOUND",
				10L,
				text,
				textBuilder.hash(text),
				SemanticMatchTextBuilder.TEXT_VERSION,
				VectorUtils.toBytes(cachedVector)
		);

		when(embeddingRepository.findByItemTypeAndItemIdIn("FOUND", List.of(10L)))
				.thenReturn(List.of(existing));

		Map<Long, float[]> result = service.getOrFetch("FOUND", List.of(item));

		assertThat(result).containsOnlyKeys(10L);
		assertThat(result.get(10L)).containsExactly(cachedVector);
		verify(embeddingClient, never()).embedItems(org.mockito.ArgumentMatchers.any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void getOrFetchRegeneratesVectorWhenVersionedHashDiffers() {
		SemanticMatchItem item = item(10L, "검정 카드지갑");
		ItemEmbedding existing = new ItemEmbedding(
				"FOUND",
				10L,
				"물품명: 오래된 텍스트",
				"old-hash",
				"semantic-text-v1",
				VectorUtils.toBytes(new float[] {0.1f})
		);
		float[] freshVector = new float[] {0.4f, 0.5f, 0.6f};

		when(embeddingRepository.findByItemTypeAndItemIdIn("FOUND", List.of(10L)))
				.thenReturn(List.of(existing));
		when(embeddingClient.embedItems(org.mockito.ArgumentMatchers.any()))
				.thenReturn(Map.of(10L, freshVector));

		Map<Long, float[]> result = service.getOrFetch("FOUND", List.of(item));

		ArgumentCaptor<List<EmbeddingItem>> captor = ArgumentCaptor.forClass(List.class);
		verify(embeddingClient).embedItems(captor.capture());

		String expectedText = textBuilder.build(item);
		assertThat(captor.getValue()).extracting(EmbeddingItem::text).containsExactly(expectedText);
		assertThat(result.get(10L)).containsExactly(freshVector);
		verify(embeddingRepository).upsert(
				eq("FOUND"),
				eq(10L),
				eq(expectedText),
				eq(textBuilder.hash(expectedText)),
				eq(SemanticMatchTextBuilder.TEXT_VERSION),
				any(byte[].class)
		);
	}

	@Test
	void getOrFetchOneUsesCachedVectorWhenVersionedHashMatches() {
		SemanticMatchItem item = item(10L, "검정 카드지갑");
		String text = textBuilder.build(item);
		float[] cachedVector = new float[] {0.7f, 0.8f};
		ItemEmbedding existing = new ItemEmbedding(
				"FOUND",
				10L,
				text,
				textBuilder.hash(text),
				SemanticMatchTextBuilder.TEXT_VERSION,
				VectorUtils.toBytes(cachedVector)
		);

		when(embeddingRepository.findByItemTypeAndItemId("FOUND", 10L))
				.thenReturn(java.util.Optional.of(existing));

		float[] result = service.getOrFetchOne("FOUND", item);

		assertThat(result).containsExactly(cachedVector);
		verify(embeddingClient, never()).embedText(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void getOrFetchOneRegeneratesVectorWhenVersionedHashDiffers() {
		SemanticMatchItem item = item(10L, "검정 카드지갑");
		ItemEmbedding existing = new ItemEmbedding(
				"FOUND",
				10L,
				"물품명: 오래된 텍스트",
				"old-hash",
				"semantic-text-v1",
				VectorUtils.toBytes(new float[] {0.1f})
		);
		float[] freshVector = new float[] {0.9f, 1.0f};

		when(embeddingRepository.findByItemTypeAndItemId("FOUND", 10L))
				.thenReturn(java.util.Optional.of(existing));
		when(embeddingClient.embedText(textBuilder.build(item)))
				.thenReturn(freshVector);

		float[] result = service.getOrFetchOne("FOUND", item);

		String expectedText = textBuilder.build(item);
		assertThat(result).containsExactly(freshVector);
		verify(embeddingRepository).upsert(
				eq("FOUND"),
				eq(10L),
				eq(expectedText),
				eq(textBuilder.hash(expectedText)),
				eq(SemanticMatchTextBuilder.TEXT_VERSION),
				any(byte[].class)
		);
	}

	private SemanticMatchItem item(Long id, String itemName) {
		return new SemanticMatchItem(
				id,
				"FOUND",
				itemName,
				"강남역 카드지갑 습득",
				"지갑",
				"검정",
				"서울특별시 강남구",
				"강남역",
				"상세 설명"
		);
	}
}
