package com.example.dogo.service.match.embedding;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnProperty(name = "match.embedding.enabled", havingValue = "true")
public class ItemEmbeddingBackfill {

	private static final Logger log = LoggerFactory.getLogger(ItemEmbeddingBackfill.class);

	@Value("${match.embedding.backfill-on-startup:false}")
	private boolean backfillOnStartup;

	@Value("${match.embedding.backfill-batch-size:100}")
	private int batchSize;

	private final ItemEmbeddingService embeddingService;
	private final LostItemRepository lostItemRepository;
	private final FoundItemRepository foundItemRepository;

	public ItemEmbeddingBackfill(ItemEmbeddingService embeddingService,
			LostItemRepository lostItemRepository,
			FoundItemRepository foundItemRepository) {
		this.embeddingService = embeddingService;
		this.lostItemRepository = lostItemRepository;
		this.foundItemRepository = foundItemRepository;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		if (!backfillOnStartup) return;
		log.info("[backfill] 임베딩 backfill 시작 (batchSize={})", batchSize);
		CompletableFuture.runAsync(this::runBackfill);
	}

	private void runBackfill() {
		try {
			int totalLost = backfillLost();
			int totalFound = backfillFound();
			log.info("[backfill] 완료 — 분실물 {}건 / 습득물 {}건 신규 임베딩", totalLost, totalFound);
		} catch (Exception e) {
			log.error("[backfill] 오류 발생", e);
		}
	}

	private int backfillLost() {
		int total = 0;
		int page = 0;
		List<LostItem> items;
		do {
			items = lostItemRepository.findByDeletedFalseOrderByLostAtDescLostIdDesc(
					PageRequest.of(page, batchSize));
			if (!items.isEmpty()) {
				int count = embeddingService.embedLostBatch(items);
				total += count;
				log.info("[backfill] 분실물 {}페이지: {}건 처리, {}건 신규 임베딩", page + 1, items.size(), count);
			}
			page++;
		} while (items.size() == batchSize);
		return total;
	}

	private int backfillFound() {
		int total = 0;
		int page = 0;
		List<FoundItem> items;
		do {
			items = foundItemRepository.findByDeletedFalseOrderByFoundAtDescFoundIdDesc(
					PageRequest.of(page, batchSize));
			if (!items.isEmpty()) {
				int count = embeddingService.embedFoundBatch(items);
				total += count;
				log.info("[backfill] 습득물 {}페이지: {}건 처리, {}건 신규 임베딩", page + 1, items.size(), count);
			}
			page++;
		} while (items.size() == batchSize);
		return total;
	}
}
