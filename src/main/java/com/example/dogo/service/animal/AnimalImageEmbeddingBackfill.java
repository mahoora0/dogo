package com.example.dogo.service.animal;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.repository.animal.AnimalReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnProperty(name = "match.pet.embedding.enabled", havingValue = "true")
public class AnimalImageEmbeddingBackfill {

	private static final Logger log = LoggerFactory.getLogger(AnimalImageEmbeddingBackfill.class);

	@Value("${match.pet.embedding.backfill-on-startup:false}")
	private boolean backfillOnStartup;

	@Value("${match.pet.embedding.force-backfill:false}")
	private boolean forceBackfill;

	@Value("${match.pet.embedding.backfill-batch-size:50}")
	private int batchSize;

	@Value("${match.pet.embedding.inference-batch-size:4}")
	private int inferenceBatchSize;

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
		log.info("[pet-embedding-backfill] scheduled (batchSize={}, inferenceBatchSize={}, forceBackfill={}, model={}, cropType={})",
				batchSize, inferenceBatchSize, forceBackfill, embeddingService.currentModelName(), embeddingService.currentCropType());
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
				List<AnimalReport> pageReports = reports.getContent();
				int chunkSize = Math.max(1, inferenceBatchSize);
				for (int start = 0; start < pageReports.size(); start += chunkSize) {
					int end = Math.min(start + chunkSize, pageReports.size());
					AnimalImageEmbeddingService.BatchEmbedStatus status =
							embeddingService.embedAndSaveBatch(pageReports.subList(start, end), forceBackfill);
					saved += status.saved();
					skipped += status.skipped();
					failed += status.failed();
				}
				log.info("[pet-embedding-backfill] page {} complete: saved={}, skipped={}, failed={}",
						page + 1, saved, skipped, failed);
				page++;
			} while (reports.hasNext());

			log.info("[pet-embedding-backfill] complete: saved={}, skipped={}, failed={}",
					saved, skipped, failed);
		} catch (Exception e) {
			log.error("[pet-embedding-backfill] failed: saved={}, skipped={}, failed={}",
					saved, skipped, failed, e);
		}
	}
}
