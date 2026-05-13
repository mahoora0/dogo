package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemSyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PoliceFoundItemSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(PoliceFoundItemSyncRunner.class);

	private final PoliceFoundItemSyncService syncService;
	private final boolean syncEnabled;
	private final boolean backfillOnStartup;

	public PoliceFoundItemSyncRunner(
			PoliceFoundItemSyncService syncService,
			@Value("${police.found-item.sync.enabled:true}") boolean syncEnabled,
			@Value("${police.found-item.backfill-on-startup:true}") boolean backfillOnStartup
	) {
		this.syncService = syncService;
		this.syncEnabled = syncEnabled;
		this.backfillOnStartup = backfillOnStartup;
	}

	public void backfillOnStartupIfEmpty() {
		if (!syncEnabled || !backfillOnStartup) {
			return;
		}

		try {
			PoliceFoundItemSyncResult result = syncService.syncBackfillLastMonth();
			log.info(
					"경찰청 습득물 초기 백필 완료. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(),
					result.savedCount(),
					result.skippedCount(),
					result.pageCount()
			);
		} catch (Exception exception) {
			log.error("경찰청 습득물 초기 백필에 실패했습니다.", exception);
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
