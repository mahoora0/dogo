package com.example.dogo.service.animal;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.service.animal.AnimalImageEmbeddingService.EmbedStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnProperty(name = "match.pet.embedding.enabled", havingValue = "true")
public class AnimalImageEmbeddingBackfill {

	private static final Logger log = LoggerFactory.getLogger(AnimalImageEmbeddingBackfill.class);

	@Value("${match.pet.embedding.backfill-on-startup:false}")
	private boolean backfillOnStartup;

	@Value("${match.pet.embedding.backfill-batch-size:50}")
	private int batchSize;

	private final AnimalImageEmbeddingService embeddingService;
	private final AnimalReportRepository reportRepository;

	public AnimalImageEmbeddingBackfill(
			AnimalImageEmbeddingService embeddingService,
			AnimalReportRepository reportRepository
	) {
		this.embeddingService = embeddingService;
		this.reportRepository = reportRepository;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		if (!backfillOnStartup) return;
		log.info("[pet-embedding-backfill] 시작 예약 (batchSize={}, model={})",
				batchSize, embeddingService.currentModelName());
		CompletableFuture.runAsync(this::runBackfill);
	}

	private void runBackfill() {
		int saved = 0;
		int skipped = 0;
		int failed = 0;
		int page = 0;

		try {
			Page<AnimalReport> reports;
			do {
				reports = reportRepository.findAll(PageRequest.of(page, batchSize));
				for (AnimalReport report : reports.getContent()) {
					if (report.isDeleted()) {
						skipped++;
						continue;
					}
					EmbedStatus status = embeddingService.embedAndSave(report);
					if (status == EmbedStatus.SAVED) saved++;
					else if (status == EmbedStatus.SKIPPED) skipped++;
					else failed++;
				}
				log.info("[pet-embedding-backfill] {}페이지 처리 완료: saved={}, skipped={}, failed={}",
						page + 1, saved, skipped, failed);
				page++;
			} while (reports.hasNext());

			log.info("[pet-embedding-backfill] 완료: saved={}, skipped={}, failed={}",
					saved, skipped, failed);
		} catch (Exception e) {
			log.error("[pet-embedding-backfill] 오류 발생: saved={}, skipped={}, failed={}",
					saved, skipped, failed, e);
		}
	}
}
