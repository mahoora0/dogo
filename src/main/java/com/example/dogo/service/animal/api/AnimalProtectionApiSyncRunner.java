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
public class AnimalProtectionApiSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(AnimalProtectionApiSyncRunner.class);
	private static final String SOURCE_TYPE = "ANIMAL_PROTECTION_API";

	private final AnimalPublicApiSyncService syncService;
	private final AnimalReportRepository animalReportRepository;
	private final boolean syncEnabled;
	private final boolean backfillOnStartup;

	public AnimalProtectionApiSyncRunner(
			AnimalProtectionApiClient client,
			AnimalPublicApiMapper mapper,
			AnimalPublicApiImageService imageService,
			AnimalReportRepository animalReportRepository,
			ApplicationEventPublisher eventPublisher,
			@Value("${animal-protection.num-of-rows:100}") int numOfRows,
			@Value("${animal-protection.incremental-lookback-days:7}") int incrementalLookbackDays,
			@Value("${animal-protection.backfill-lookback-days:30}") int backfillLookbackDays,
			@Value("${animal-protection.sync.enabled:false}") boolean syncEnabled,
			@Value("${animal-protection.backfill-on-startup:false}") boolean backfillOnStartup
	) {
		this.syncService = new AnimalPublicApiSyncService(
				client,
				mapper,
				imageService,
				animalReportRepository,
				eventPublisher,
				SOURCE_TYPE,
				"SIGHTING",
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
			log.info("Animal protection API backfill completed. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(), result.savedCount(), result.skippedCount(), result.pageCount());
		} catch (Exception exception) {
			log.error("Animal protection API backfill failed.", exception);
		}
	}

	@Scheduled(cron = "${animal-protection.sync-cron:0 30 * * * *}")
	public void syncIncremental() {
		if (!syncEnabled) {
			return;
		}
		try {
			AnimalPublicApiSyncResult result = syncService.syncIncremental();
			log.info("Animal protection API incremental sync completed. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(), result.savedCount(), result.skippedCount(), result.pageCount());
		} catch (Exception exception) {
			log.error("Animal protection API incremental sync failed.", exception);
		}
	}
}
