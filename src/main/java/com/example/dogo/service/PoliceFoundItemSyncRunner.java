package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemSyncResult;
import com.example.dogo.repository.FoundItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

	public PoliceFoundItemSyncRunner(
			PoliceFoundItemSyncService syncService,
			FoundItemRepository foundItemRepository,
			@Value("${police.found-item.sync.enabled:true}") boolean syncEnabled,
			@Value("${police.found-item.backfill-on-startup:true}") boolean backfillOnStartup
	) {
		this.syncService = syncService;
		this.foundItemRepository = foundItemRepository;
		this.syncEnabled = syncEnabled;
		this.backfillOnStartup = backfillOnStartup;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void backfillOnStartupIfEmpty() {
		if (!syncEnabled || !backfillOnStartup) {
			return;
		}
		if (foundItemRepository.existsBySourceType(POLICE_SOURCE_TYPE)) {
			log.info("경찰청 습득물 초기 백필을 건너뜁니다. 이미 저장된 경찰청 데이터가 있습니다.");
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
