package com.example.dogo.service.missing;

import com.example.dogo.repository.missing.MissingPersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MissingAlertSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(MissingAlertSyncRunner.class);
	private static final String PUBLIC_API_SOURCE_TYPE = "PUBLIC_API";

	private final MissingAlertSyncService syncService;
	private final MissingPersonRepository missingPersonRepository;
	private final boolean syncEnabled;
	private final boolean backfillOnStartup;

	public MissingAlertSyncRunner(
			MissingAlertSyncService syncService,
			MissingPersonRepository missingPersonRepository,
			@Value("${safe182.missing-alert.sync.enabled:false}") boolean syncEnabled,
			@Value("${safe182.missing-alert.backfill-on-startup:false}") boolean backfillOnStartup
	) {
		this.syncService = syncService;
		this.missingPersonRepository = missingPersonRepository;
		this.syncEnabled = syncEnabled;
		this.backfillOnStartup = backfillOnStartup;
	}

	public void backfillOnStartupIfEmpty() {
		if (!syncEnabled || !backfillOnStartup) {
			return;
		}
		if (missingPersonRepository.existsBySourceType(PUBLIC_API_SOURCE_TYPE)) {
			log.info("안전Dream 실종 경보 초기 동기화를 건너뜁니다. 이미 저장된 공공데이터가 있습니다.");
			return;
		}

		try {
			MissingAlertSyncResult result = syncService.syncBackfillFromToday();
			log.info(
					"안전Dream 실종 경보 초기 동기화 완료. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(),
					result.savedCount(),
					result.skippedCount(),
					result.pageCount()
			);
		} catch (Exception exception) {
			log.error("안전Dream 실종 경보 초기 동기화에 실패했습니다.", exception);
		}
	}

	@Scheduled(cron = "${safe182.missing-alert.sync-cron:0 20 * * * *}")
	public void syncIncremental() {
		if (!syncEnabled) {
			return;
		}

		try {
			MissingAlertSyncResult result = syncService.syncIncrementalFromToday();
			log.info(
					"안전Dream 실종 경보 증분 동기화 완료. fetched={}, saved={}, skipped={}, pages={}",
					result.fetchedCount(),
					result.savedCount(),
					result.skippedCount(),
					result.pageCount()
			);
		} catch (Exception exception) {
			log.error("안전Dream 실종 경보 증분 동기화에 실패했습니다.", exception);
		}
	}
}
