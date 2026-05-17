package com.example.dogo.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseMatchRegistry {

	private static final Logger log = LoggerFactory.getLogger(SseMatchRegistry.class);
	private static final long COMPLETED_FRAGMENT_TTL_SECONDS = 120L;

	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
	private final Map<String, CompletedFragment> completedFragments = new ConcurrentHashMap<>();

	public SseEmitter register(String key) {
		CompletedFragment completed = completedFragments.remove(key);
		if (completed != null && !completed.isExpired()) {
			return immediateEmitter(completed.htmlFragment());
		}

		SseEmitter emitter = new SseEmitter(90_000L);
		emitters.put(key, emitter);
		emitter.onCompletion(() -> emitters.remove(key));
		emitter.onTimeout(() -> emitters.remove(key));
		emitter.onError(e -> emitters.remove(key));
		return emitter;
	}

	public void push(String key, String htmlFragment) {
		SseEmitter emitter = emitters.remove(key);
		if (emitter == null) {
			completedFragments.put(key, new CompletedFragment(htmlFragment, Instant.now()));
			removeExpiredFragments();
			return;
		}
		try {
			emitter.send(SseEmitter.event().name("match-ready").data(htmlFragment));
			emitter.complete();
		} catch (IOException e) {
			log.warn("SSE 전송 실패: key={}", key, e);
			emitter.completeWithError(e);
		}
	}

	private SseEmitter immediateEmitter(String htmlFragment) {
		SseEmitter emitter = new SseEmitter(0L);
		try {
			emitter.send(SseEmitter.event().name("match-ready").data(htmlFragment));
			emitter.complete();
		} catch (IOException e) {
			emitter.completeWithError(e);
		}
		return emitter;
	}

	private void removeExpiredFragments() {
		completedFragments.entrySet().removeIf(entry -> entry.getValue().isExpired());
	}

	private record CompletedFragment(String htmlFragment, Instant createdAt) {
		boolean isExpired() {
			return createdAt.plusSeconds(COMPLETED_FRAGMENT_TTL_SECONDS).isBefore(Instant.now());
		}
	}
}
