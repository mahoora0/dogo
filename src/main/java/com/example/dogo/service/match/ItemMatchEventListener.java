package com.example.dogo.service.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ItemMatchEventListener {

	private static final Logger log = LoggerFactory.getLogger(ItemMatchEventListener.class);

	private final ItemMatchService itemMatchService;

	public ItemMatchEventListener(ItemMatchService itemMatchService) {
		this.itemMatchService = itemMatchService;
	}

	@EventListener
	public void handleLostItemMatchRequested(LostItemMatchRequestedEvent event) {
		try {
			itemMatchService.matchForLostItemId(event.lostId());
		} catch (Exception exception) {
			log.warn("분실물 매칭 실행 중 오류가 발생했습니다. lostId={}", event.lostId(), exception);
		}
	}

	@EventListener
	public void handleFoundItemMatchRequested(FoundItemMatchRequestedEvent event) {
		try {
			itemMatchService.matchForFoundItemId(event.foundId());
		} catch (Exception exception) {
			log.warn("습득물 매칭 실행 중 오류가 발생했습니다. foundId={}", event.foundId(), exception);
		}
	}
}
