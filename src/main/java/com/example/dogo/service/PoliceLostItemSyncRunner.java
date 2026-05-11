package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemSyncResult;
import com.example.dogo.repository.LostItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PoliceLostItemSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(PoliceLostItemSyncRunner.class);
	private static final String POLICE_SOURCE_TYPE = "POLICE";

	private final PoliceLostItemSyncService syncService;
	private final LostItemRepository lostItemRepository;
	private final boolean syncEnabled;
	private final boolean backfillOnStartup;

	public PoliceLostItemSyncRunner(
			PoliceLostItemSyncService syncService,
			LostItemRepository lostItemRepository,
			@Value("${police.lost-item.sync.enabled:true}") boolean syncEnabled,
			@Value("${police.lost-item.backfill-on-startup:true}") boolean backfillOnStartup
	) {
		this.syncService = syncService;
		this.lostItemRepository = lostItemRepository;
		this.syncEnabled = syncEnabled;
		this.backfillOnStartup = backfillOnStartup;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void backfillOnStartupIfEmpty() {
		if (!syncEnabled || !backfillOnStartup) {
			return;
		}
		if (lostItemRepository.existsBySourceType(POLICE_SOURCE_TYPE)) {
			log.info("경찰청 분실물 초기 백필을 건너뜁니다. 이미 저장된 경찰청 데이터가 있습니다.");
			return;
		}

		try {
			PoliceLostItemSyncResult result = syncService.syncBackfillLastMonth();
			log.info(
					"경찰청 분실물 초기 백필 완료. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(),
					result.savedCount(),
					result.skippedCount(),
					result.pageCount()
			);
		} catch (Exception exception) {
			log.error("경찰청 분실물 초기 백필에 실패했습니다.", exception);
		}
	}

	@Scheduled(cron = "${police.lost-item.sync-cron:0 0 * * * *}")
	public void syncIncremental() {
		if (!syncEnabled) {
			return;
		}

		try {
			PoliceLostItemSyncResult result = syncService.syncIncrementalLastMonth();
			log.info(
					"경찰청 분실물 증분 동기화 완료. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(),
					result.savedCount(),
					result.skippedCount(),
					result.pageCount()
			);
		} catch (Exception exception) {
			log.error("경찰청 분실물 증분 동기화에 실패했습니다.", exception);
		}
	}
}
