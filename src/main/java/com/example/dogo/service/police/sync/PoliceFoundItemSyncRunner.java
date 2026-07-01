package com.example.dogo.service.police.sync;

import com.example.dogo.dto.police.PoliceFoundItemSyncResult;
import com.example.dogo.repository.item.FoundItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PoliceFoundItemSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(PoliceFoundItemSyncRunner.class);
	private static final String POLICE_SOURCE_TYPE = "POLICE";

	private final PoliceFoundItemSyncService syncService;
	private final FoundItemRepository foundItemRepository;
	private final boolean syncEnabled;
	private final boolean backfillOnStartup;
	private final boolean forceBackfill;

	public PoliceFoundItemSyncRunner(
			PoliceFoundItemSyncService syncService,
			FoundItemRepository foundItemRepository,
			@Value("${police.found-item.sync.enabled:true}") boolean syncEnabled,
			@Value("${police.found-item.backfill-on-startup:true}") boolean backfillOnStartup,
			@Value("${police.found-item.force-backfill:false}") boolean forceBackfill
	) {
		this.syncService = syncService;
		this.foundItemRepository = foundItemRepository;
		this.syncEnabled = syncEnabled;
		this.backfillOnStartup = backfillOnStartup;
		this.forceBackfill = forceBackfill;
	}

	public void backfillOnStartupIfEmpty() {
		if (!syncEnabled || !backfillOnStartup) {
			return;
		}

		try {
			boolean hasData = foundItemRepository.existsBySourceType(POLICE_SOURCE_TYPE);
			boolean runFullBackfill = forceBackfill || !hasData;
			PoliceFoundItemSyncResult result = runFullBackfill
					? syncService.syncBackfillLastMonth()
					: syncService.syncIncrementalLastMonth();
			log.info(
					"경찰청 습득물 초기 {} 완료. fetched={}, saved={}, skipped={}, pages={}",
					runFullBackfill ? "백필" : "증분 동기화",
					result.fetchedCount(),
					result.savedCount(),
					result.skippedCount(),
					result.pageCount()
			);
		} catch (Exception exception) {
			log.error("경찰청 습득물 초기 동기화에 실패했습니다.", exception);
		}
	}

	@Scheduled(cron = "${police.found-item.sync-cron:0 10 * * * *}")
	public void syncIncremental() {
		if (!syncEnabled) {
			return;
		}

		try {
			PoliceFoundItemSyncResult result = syncService.syncIncrementalLastMonth();
			log.info(
					"경찰청 습득물 증분 동기화 완료. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(),
					result.savedCount(),
					result.skippedCount(),
					result.pageCount()
			);
		} catch (Exception exception) {
			log.error("경찰청 습득물 증분 동기화에 실패했습니다.", exception);
		}
	}
}
