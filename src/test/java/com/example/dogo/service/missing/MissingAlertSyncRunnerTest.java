package com.example.dogo.service.missing;

import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.service.missing.client.Safe182MissingPersonClient;
import com.example.dogo.service.missing.client.Safe182MissingPersonPage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;

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
	void backfillOnStartupRunsIncrementalWhenPublicApiDataAlreadyExists() {
		RecordingMissingAlertSyncService syncService = new RecordingMissingAlertSyncService();
		MissingAlertSyncRunner runner = new MissingAlertSyncRunner(
				syncService,
				repository(true),
				true,
				true
		);

		runner.backfillOnStartupIfEmpty();

		assertThat(syncService.backfillCount).isZero();
		assertThat(syncService.incrementalCount).isEqualTo(1);
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
					new Safe182MissingPersonClient() {
						@Override
						public Safe182MissingPersonPage search(String keyword, int page, int rowSize) {
							return null;
						}

						@Override
						public Safe182MissingPersonPage searchByDateRange(LocalDate startDate, LocalDate endDate, int page, int rowSize) {
							return null;
						}
					},
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
