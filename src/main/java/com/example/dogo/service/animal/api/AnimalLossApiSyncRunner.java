package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiSyncResult;
import com.example.dogo.repository.animal.AnimalReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnimalLossApiSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(AnimalLossApiSyncRunner.class);
	private static final String SOURCE_TYPE = "ANIMAL_LOSS_API";

	private final AnimalPublicApiSyncService syncService;
	private final AnimalReportRepository animalReportRepository;
	private final boolean syncEnabled;
	private final boolean backfillOnStartup;

	public AnimalLossApiSyncRunner(
			AnimalLossApiClient client,
			AnimalPublicApiMapper mapper,
			AnimalPublicApiImageService imageService,
			AnimalReportRepository animalReportRepository,
			ApplicationEventPublisher eventPublisher,
			@Value("${animal-loss.num-of-rows:100}") int numOfRows,
			@Value("${animal-loss.incremental-lookback-days:7}") int incrementalLookbackDays,
			@Value("${animal-loss.backfill-lookback-days:30}") int backfillLookbackDays,
			@Value("${animal-loss.sync.enabled:false}") boolean syncEnabled,
			@Value("${animal-loss.backfill-on-startup:false}") boolean backfillOnStartup
	) {
		this.syncService = new AnimalPublicApiSyncService(
				client,
				mapper,
				imageService,
				animalReportRepository,
				eventPublisher,
				SOURCE_TYPE,
				"MISSING",
				numOfRows,
				incrementalLookbackDays,
				backfillLookbackDays
		);
		this.animalReportRepository = animalReportRepository;
		this.syncEnabled = syncEnabled;
		this.backfillOnStartup = backfillOnStartup;
	}

	public void backfillOnStartupIfEmpty() {
		if (!syncEnabled || !backfillOnStartup || animalReportRepository.existsBySourceType(SOURCE_TYPE)) {
			return;
		}
		try {
			AnimalPublicApiSyncResult result = syncService.syncBackfill();
			log.info("Animal loss API backfill completed. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(), result.savedCount(), result.skippedCount(), result.pageCount());
		} catch (Exception exception) {
			log.error("Animal loss API backfill failed.", exception);
		}
	}

	@Scheduled(cron = "${animal-loss.sync-cron:0 40 * * * *}")
	public void syncIncremental() {
		if (!syncEnabled) {
			return;
		}
		try {
			AnimalPublicApiSyncResult result = syncService.syncIncremental();
			log.info("Animal loss API incremental sync completed. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(), result.savedCount(), result.skippedCount(), result.pageCount());
		} catch (Exception exception) {
			log.error("Animal loss API incremental sync failed.", exception);
		}
	}
}
