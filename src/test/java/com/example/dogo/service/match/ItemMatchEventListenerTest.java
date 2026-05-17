package com.example.dogo.service.match;

import org.junit.jupiter.api.Test;

import com.example.dogo.sse.SseMatchRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemMatchEventListenerTest {

	@Test
	void lostItemEventDelegatesToMatchServiceById() {
		ItemMatchService itemMatchService = mock(ItemMatchService.class);
		MatchFragmentRenderer fragmentRenderer = mock(MatchFragmentRenderer.class);
		SseFixture fixture = new SseFixture(itemMatchService, fragmentRenderer);
		when(itemMatchService.getMatchesForLostItem(10L)).thenReturn(List.of());
		when(fragmentRenderer.renderLostItemMatches(List.of())).thenReturn("<div></div>");

		fixture.listener().handleLostItemMatchRequested(new LostItemMatchRequestedEvent(10L));

		verify(itemMatchService).matchForLostItemId(10L);
		verify(fixture.sseMatchRegistry()).push("lost:10", "<div></div>");
	}

	@Test
	void foundItemEventDelegatesToMatchServiceById() {
		ItemMatchService itemMatchService = mock(ItemMatchService.class);
		MatchFragmentRenderer fragmentRenderer = mock(MatchFragmentRenderer.class);
		SseFixture fixture = new SseFixture(itemMatchService, fragmentRenderer);
		when(itemMatchService.getMatchesForFoundItem(20L)).thenReturn(List.of());
		when(fragmentRenderer.renderFoundItemMatches(List.of())).thenReturn("<div></div>");

		fixture.listener().handleFoundItemMatchRequested(new FoundItemMatchRequestedEvent(20L));

		verify(itemMatchService).matchForFoundItemId(20L);
		verify(fixture.sseMatchRegistry()).push("found:20", "<div></div>");
	}

	@Test
	void listenerSwallowsMatchFailuresAfterLogging() {
		ItemMatchService itemMatchService = mock(ItemMatchService.class);
		doThrow(new IllegalStateException("boom")).when(itemMatchService).matchForLostItemId(10L);
		ItemMatchEventListener listener = new SseFixture(itemMatchService, mock(MatchFragmentRenderer.class)).listener();

		assertThatCode(() -> listener.handleLostItemMatchRequested(new LostItemMatchRequestedEvent(10L)))
				.doesNotThrowAnyException();
	}

	private record SseFixture(ItemMatchEventListener listener, SseMatchRegistry sseMatchRegistry) {
		SseFixture(ItemMatchService itemMatchService, MatchFragmentRenderer fragmentRenderer) {
			this(itemMatchService, fragmentRenderer, mock(SseMatchRegistry.class));
		}

		private SseFixture(
				ItemMatchService itemMatchService,
				MatchFragmentRenderer fragmentRenderer,
				SseMatchRegistry sseMatchRegistry
		) {
			this(new ItemMatchEventListener(itemMatchService, fragmentRenderer, sseMatchRegistry), sseMatchRegistry);
		}
	}
}
