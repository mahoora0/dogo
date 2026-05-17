package com.example.dogo.service.animal;

import com.example.dogo.dto.animal.AnimalMatchCandidateView;
import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.service.match.MatchFragmentRenderer;
import com.example.dogo.sse.SseMatchRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@ConditionalOnProperty(name = "match.pet.embedding.enabled", havingValue = "true")
public class AnimalReportEventListener {

	private static final Logger log = LoggerFactory.getLogger(AnimalReportEventListener.class);

	private final AnimalReportRepository reportRepository;
	private final AnimalImageEmbeddingService embeddingService;
	private final AnimalMatchService matchService;
	private final AnimalReportService animalReportService;
	private final MatchFragmentRenderer fragmentRenderer;
	private final SseMatchRegistry sseMatchRegistry;

	public AnimalReportEventListener(
			AnimalReportRepository reportRepository,
			AnimalImageEmbeddingService embeddingService,
			AnimalMatchService matchService,
			AnimalReportService animalReportService,
			MatchFragmentRenderer fragmentRenderer,
			SseMatchRegistry sseMatchRegistry
	) {
		this.reportRepository = reportRepository;
		this.embeddingService = embeddingService;
		this.matchService = matchService;
		this.animalReportService = animalReportService;
		this.fragmentRenderer = fragmentRenderer;
		this.sseMatchRegistry = sseMatchRegistry;
	}

	@Async("itemMatchTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleAnimalReportCreated(AnimalReportCreatedEvent event) {
		AnimalReport report = reportRepository.findById(event.reportId()).orElse(null);
		if (report == null) return;
		try {
			embeddingService.embedAndSave(report);
			matchService.matchForReport(report);
			String reportType = report.getReportType();
			List<AnimalMatchCandidateView> candidates =
					animalReportService.getMatchCandidates(event.reportId(), reportType);
			String html = fragmentRenderer.renderAnimalReportMatches(candidates, reportType);
			sseMatchRegistry.push("animal:" + event.reportId(), html);
		} catch (Exception e) {
			log.warn("[pet-match] 이벤트 처리 실패: reportId={}", event.reportId(), e);
		}
	}
}
