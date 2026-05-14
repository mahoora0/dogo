package com.example.dogo.service.match;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ItemMatchEventListenerTest {

	@Test
	void lostItemEventDelegatesToMatchServiceById() {
		ItemMatchService itemMatchService = mock(ItemMatchService.class);
		ItemMatchEventListener listener = new ItemMatchEventListener(itemMatchService);

		listener.handleLostItemMatchRequested(new LostItemMatchRequestedEvent(10L));

		verify(itemMatchService).matchForLostItemId(10L);
	}

	@Test
	void foundItemEventDelegatesToMatchServiceById() {
		ItemMatchService itemMatchService = mock(ItemMatchService.class);
		ItemMatchEventListener listener = new ItemMatchEventListener(itemMatchService);

		listener.handleFoundItemMatchRequested(new FoundItemMatchRequestedEvent(20L));

		verify(itemMatchService).matchForFoundItemId(20L);
	}

	@Test
	void listenerSwallowsMatchFailuresAfterLogging() {
		ItemMatchService itemMatchService = mock(ItemMatchService.class);
		doThrow(new IllegalStateException("boom")).when(itemMatchService).matchForLostItemId(10L);
		ItemMatchEventListener listener = new ItemMatchEventListener(itemMatchService);

		assertThatCode(() -> listener.handleLostItemMatchRequested(new LostItemMatchRequestedEvent(10L)))
				.doesNotThrowAnyException();
	}
}
