package com.example.dogo.service.match;

import com.example.dogo.sse.SseMatchRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ItemMatchEventListener {

	private static final Logger log = LoggerFactory.getLogger(ItemMatchEventListener.class);

	private final ItemMatchService itemMatchService;
	private final MatchFragmentRenderer fragmentRenderer;
	private final SseMatchRegistry sseMatchRegistry;

	public ItemMatchEventListener(ItemMatchService itemMatchService,
								  MatchFragmentRenderer fragmentRenderer,
								  SseMatchRegistry sseMatchRegistry) {
		this.itemMatchService = itemMatchService;
		this.fragmentRenderer = fragmentRenderer;
		this.sseMatchRegistry = sseMatchRegistry;
	}

	@EventListener
	public void handleLostItemMatchRequested(LostItemMatchRequestedEvent event) {
		try {
			itemMatchService.matchForLostItemId(event.lostId());
			String html = fragmentRenderer.renderLostItemMatches(
					itemMatchService.getMatchesForLostItem(event.lostId()));
			sseMatchRegistry.push("lost:" + event.lostId(), html);
		} catch (Exception exception) {
			log.warn("분실물 매칭 실행 중 오류가 발생했습니다. lostId={}", event.lostId(), exception);
		}
	}

	@EventListener
	public void handleFoundItemMatchRequested(FoundItemMatchRequestedEvent event) {
		try {
			itemMatchService.matchForFoundItemId(event.foundId());
			String html = fragmentRenderer.renderFoundItemMatches(
					itemMatchService.getMatchesForFoundItem(event.foundId()));
			sseMatchRegistry.push("found:" + event.foundId(), html);
		} catch (Exception exception) {
			log.warn("습득물 매칭 실행 중 오류가 발생했습니다. foundId={}", event.foundId(), exception);
		}
	}
}
