package com.example.dogo.controller.sse;

import com.example.dogo.dto.animal.AnimalMatchCandidateView;
import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.service.animal.AnimalReportService;
import com.example.dogo.service.match.ItemMatchService;
import com.example.dogo.service.match.MatchFragmentRenderer;
import com.example.dogo.sse.SseMatchRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SseMatchController {

	private final SseMatchRegistry registry;
	private final ItemMatchService itemMatchService;
	private final MatchFragmentRenderer fragmentRenderer;
	private final AnimalReportService animalReportService;

	@GetMapping(value = "/lost-items/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamLostItemMatches(@PathVariable Long id) {
		List<MatchCandidateView> existing = itemMatchService.getMatchesForLostItem(id);
		if (!existing.isEmpty()) {
			return immediateEmitter(fragmentRenderer.renderLostItemMatches(existing));
		}
		return registry.register("lost:" + id);
	}

	@GetMapping(value = "/found-items/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamFoundItemMatches(@PathVariable Long id) {
		List<MatchCandidateView> existing = itemMatchService.getMatchesForFoundItem(id);
		if (!existing.isEmpty()) {
			return immediateEmitter(fragmentRenderer.renderFoundItemMatches(existing));
		}
		return registry.register("found:" + id);
	}

	@GetMapping(value = "/animal-reports/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamAnimalReportMatches(@PathVariable Long id) {
		var report = animalReportService.getDetail(id);
		List<AnimalMatchCandidateView> existing = animalReportService.getMatchCandidates(id, report.reportType());
		if (!existing.isEmpty()) {
			return immediateEmitter(fragmentRenderer.renderAnimalReportMatches(existing, report.reportType()));
		}
		return registry.register("animal:" + id);
	}

	private SseEmitter immediateEmitter(String html) {
		SseEmitter emitter = new SseEmitter(0L);
		try {
			emitter.send(SseEmitter.event().name("match-ready").data(html));
			emitter.complete();
		} catch (IOException e) {
			emitter.completeWithError(e);
		}
		return emitter;
	}
}
