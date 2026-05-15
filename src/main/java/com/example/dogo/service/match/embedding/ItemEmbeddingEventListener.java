package com.example.dogo.service.match.embedding;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "match.embedding.enabled", havingValue = "true")
public class ItemEmbeddingEventListener {

	private static final Logger log = LoggerFactory.getLogger(ItemEmbeddingEventListener.class);

	private final ItemEmbeddingService embeddingService;
	private final LostItemRepository lostItemRepository;
	private final FoundItemRepository foundItemRepository;

	public ItemEmbeddingEventListener(ItemEmbeddingService embeddingService,
			LostItemRepository lostItemRepository,
			FoundItemRepository foundItemRepository) {
		this.embeddingService = embeddingService;
		this.lostItemRepository = lostItemRepository;
		this.foundItemRepository = foundItemRepository;
	}

	@Async("itemMatchTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleLostItemEmbeddingRequested(LostItemEmbeddingRequestedEvent event) {
		try {
			LostItem item = lostItemRepository.findById(event.lostId()).orElse(null);
			if (item == null) return;
			embeddingService.embedAndSave(item);
		} catch (Exception e) {
			log.warn("[embedding] 분실물 임베딩 실패: lostId={}", event.lostId(), e);
		}
	}

	@Async("itemMatchTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleFoundItemEmbeddingRequested(FoundItemEmbeddingRequestedEvent event) {
		try {
			FoundItem item = foundItemRepository.findById(event.foundId()).orElse(null);
			if (item == null) return;
			embeddingService.embedAndSave(item);
		} catch (Exception e) {
			log.warn("[embedding] 습득물 임베딩 실패: foundId={}", event.foundId(), e);
		}
	}
}
