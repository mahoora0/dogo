package com.example.dogo.service.match;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.service.match.embedding.ItemEmbeddingService;
import com.example.dogo.sse.SseMatchRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ItemMatchEventListener {

	private static final Logger log = LoggerFactory.getLogger(ItemMatchEventListener.class);

	private final ItemMatchService itemMatchService;
	private final MatchFragmentRenderer fragmentRenderer;
	private final SseMatchRegistry sseMatchRegistry;
	private final LostItemRepository lostItemRepository;
	private final FoundItemRepository foundItemRepository;
	private final ItemEmbeddingService embeddingService;

	public ItemMatchEventListener(ItemMatchService itemMatchService,
								  MatchFragmentRenderer fragmentRenderer,
								  SseMatchRegistry sseMatchRegistry,
								  LostItemRepository lostItemRepository,
								  FoundItemRepository foundItemRepository,
								  @Nullable ItemEmbeddingService embeddingService) {
		this.itemMatchService = itemMatchService;
		this.fragmentRenderer = fragmentRenderer;
		this.sseMatchRegistry = sseMatchRegistry;
		this.lostItemRepository = lostItemRepository;
		this.foundItemRepository = foundItemRepository;
		this.embeddingService = embeddingService;
	}

	@Async("itemMatchTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleLostItemMatchRequested(LostItemMatchRequestedEvent event) {
		try {
			embedLostItemIfEnabled(event.lostId());
			itemMatchService.matchForLostItemId(event.lostId());
			String html = fragmentRenderer.renderLostItemMatches(
					itemMatchService.getMatchesForLostItemPreview(event.lostId()));
			sseMatchRegistry.push("lost:" + event.lostId(), html);
		} catch (Exception exception) {
			log.warn("분실물 매칭 실행 중 오류가 발생했습니다. lostId={}", event.lostId(), exception);
		}
	}

	@Async("itemMatchTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleFoundItemMatchRequested(FoundItemMatchRequestedEvent event) {
		try {
			embedFoundItemIfEnabled(event.foundId());
			itemMatchService.matchForFoundItemId(event.foundId());
			String html = fragmentRenderer.renderFoundItemMatches(
					itemMatchService.getMatchesForFoundItem(event.foundId()));
			sseMatchRegistry.push("found:" + event.foundId(), html);
		} catch (Exception exception) {
			log.warn("습득물 매칭 실행 중 오류가 발생했습니다. foundId={}", event.foundId(), exception);
		}
	}

	private void embedLostItemIfEnabled(Long lostId) {
		if (embeddingService == null) {
			return;
		}
		LostItem item = lostItemRepository.findById(lostId).orElse(null);
		if (item != null) {
			embeddingService.embedAndSave(item);
		}
	}

	private void embedFoundItemIfEnabled(Long foundId) {
		if (embeddingService == null) {
			return;
		}
		FoundItem item = foundItemRepository.findById(foundId).orElse(null);
		if (item != null) {
			embeddingService.embedAndSave(item);
		}
	}
}
