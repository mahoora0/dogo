package com.example.dogo.service.missing;

import com.example.dogo.repository.missing.MissingPersonRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

class MissingAlertSyncRunnerTest {

	@Test
	void backfillOnStartupRunsWhenPublicApiDataIsEmpty() {
		RecordingMissingAlertSyncService syncService = new RecordingMissingAlertSyncService();
		MissingAlertSyncRunner runner = new MissingAlertSyncRunner(
				syncService,
				repository(false),
				true,
				true
		);

		runner.backfillOnStartupIfEmpty();

		assertThat(syncService.backfillCount).isEqualTo(1);
	}

	@Test
	void backfillOnStartupSkipsWhenPublicApiDataAlreadyExists() {
		RecordingMissingAlertSyncService syncService = new RecordingMissingAlertSyncService();
		MissingAlertSyncRunner runner = new MissingAlertSyncRunner(
				syncService,
				repository(true),
				true,
				true
		);

		runner.backfillOnStartupIfEmpty();

		assertThat(syncService.backfillCount).isZero();
	}

	@Test
	void scheduledSyncRunsWhenEnabled() {
		RecordingMissingAlertSyncService syncService = new RecordingMissingAlertSyncService();
		MissingAlertSyncRunner runner = new MissingAlertSyncRunner(
				syncService,
				repository(false),
				true,
				false
		);

		runner.syncIncremental();

		assertThat(syncService.incrementalCount).isEqualTo(1);
	}

	@Test
	void scheduledSyncSkipsWhenDisabled() {
		RecordingMissingAlertSyncService syncService = new RecordingMissingAlertSyncService();
		MissingAlertSyncRunner runner = new MissingAlertSyncRunner(
				syncService,
				repository(false),
				false,
				true
		);

		runner.syncIncremental();

		assertThat(syncService.incrementalCount).isZero();
	}

	private MissingPersonRepository repository(boolean existsBySourceType) {
		return (MissingPersonRepository) Proxy.newProxyInstance(
				MissingPersonRepository.class.getClassLoader(),
				new Class<?>[] { MissingPersonRepository.class },
				(proxy, method, args) -> {
					if ("existsBySourceType".equals(method.getName())) {
						return existsBySourceType;
					}
					throw new UnsupportedOperationException(method.getName());
				}
		);
	}

	private static class RecordingMissingAlertSyncService extends MissingAlertSyncService {
		private int backfillCount;
		private int incrementalCount;

		RecordingMissingAlertSyncService() {
			super(
					new MissingAlertService((occurrenceDate, rowSize, page) -> null),
					null,
					100,
					1,
					7
			);
		}

		@Override
		public MissingAlertSyncResult syncBackfillFromToday() {
			backfillCount++;
			return new MissingAlertSyncResult(0, 0, 0, 0);
		}

		@Override
		public MissingAlertSyncResult syncIncrementalFromToday() {
			incrementalCount++;
			return new MissingAlertSyncResult(0, 0, 0, 0);
		}
	}
}
