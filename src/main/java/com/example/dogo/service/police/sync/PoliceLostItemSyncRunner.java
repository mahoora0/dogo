package com.example.dogo.service.police.sync;

import com.example.dogo.dto.police.PoliceLostItemSyncResult;
import com.example.dogo.repository.item.LostItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
	private final boolean forceBackfill;

	public PoliceLostItemSyncRunner(
			PoliceLostItemSyncService syncService,
			LostItemRepository lostItemRepository,
			@Value("${police.lost-item.sync.enabled:true}") boolean syncEnabled,
			@Value("${police.lost-item.backfill-on-startup:true}") boolean backfillOnStartup,
			@Value("${police.lost-item.force-backfill:false}") boolean forceBackfill
	) {
		this.syncService = syncService;
		this.lostItemRepository = lostItemRepository;
		this.syncEnabled = syncEnabled;
		this.backfillOnStartup = backfillOnStartup;
		this.forceBackfill = forceBackfill;
	}

	public void backfillOnStartupIfEmpty() {
		if (!syncEnabled || !backfillOnStartup) {
			return;
		}

		try {
			boolean hasData = lostItemRepository.existsBySourceType(POLICE_SOURCE_TYPE);
			boolean runFullBackfill = forceBackfill || !hasData;
			PoliceLostItemSyncResult result = runFullBackfill
					? syncService.syncBackfillLastMonth()
					: syncService.syncIncrementalLastMonth();
			log.info(
					"경찰청 분실물 초기 {} 완료. fetched={}, saved={}, skipped={}, pages={}",
					runFullBackfill ? "백필" : "증분 동기화",
					result.fetchedCount(),
					result.savedCount(),
					result.skippedCount(),
					result.pageCount()
			);
		} catch (Exception exception) {
			log.error("경찰청 분실물 초기 동기화에 실패했습니다.", exception);
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
