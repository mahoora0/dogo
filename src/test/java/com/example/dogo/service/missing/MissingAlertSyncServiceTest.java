package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;
import com.example.dogo.dto.missing.Safe182AmberAlertView;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.missing.MissingPersonRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissingAlertSyncServiceTest {

	@Test
	void syncDateStoresPublicApiMissingReports() {
		RecordingMissingAlertService missingAlertService = new RecordingMissingAlertService(
				new Safe182AmberAlertPage("00", "OK", 1, List.of(alert()))
		);
		RecordingMissingPersonRepository repository = new RecordingMissingPersonRepository(false, false);
		MissingAlertSyncService syncService = syncService(missingAlertService, repository.proxy());

		MissingAlertSyncResult result = syncService.syncDate(LocalDate.of(2026, 5, 18));

		assertThat(result.fetchedCount()).isEqualTo(1);
		assertThat(result.savedCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isZero();
		assertThat(repository.savedReports).hasSize(1);
		assertThat(repository.savedReports.get(0).getSourceType()).isEqualTo("PUBLIC_API");
		assertThat(repository.savedReports.get(0).getApiProvider()).isEqualTo("SAFE182_AMBER");
	}

	@Test
	void syncDateSkipsExistingPublicApiReports() {
		RecordingMissingAlertService missingAlertService = new RecordingMissingAlertService(
				new Safe182AmberAlertPage("00", "OK", 1, List.of(alert()))
		);
		RecordingMissingPersonRepository repository = new RecordingMissingPersonRepository(true, false);
		MissingAlertSyncService syncService = syncService(missingAlertService, repository.proxy());

		MissingAlertSyncResult result = syncService.syncDate(LocalDate.of(2026, 5, 18));

		assertThat(result.fetchedCount()).isEqualTo(1);
		assertThat(result.savedCount()).isZero();
		assertThat(result.skippedCount()).isEqualTo(1);
		assertThat(repository.savedReports).isEmpty();
	}

	@Test
	void syncBackfillFetchesConfiguredLookbackRange() {
		RecordingMissingAlertService missingAlertService = new RecordingMissingAlertService(
				new Safe182AmberAlertPage("00", "OK", 0, List.of())
		);
		RecordingMissingPersonRepository repository = new RecordingMissingPersonRepository(false, false);
		MissingAlertSyncService syncService = syncService(missingAlertService, repository.proxy());

		MissingAlertSyncResult result = syncService.syncBackfill(LocalDate.of(2026, 5, 18));

		assertThat(result.pageCount()).isEqualTo(2);
		assertThat(missingAlertService.requestedDates).containsExactly(
				LocalDate.of(2026, 5, 17),
				LocalDate.of(2026, 5, 18)
		);
	}

	private MissingAlertSyncService syncService(MissingAlertService service, MissingPersonRepository repository) {
		return new MissingAlertSyncService(
				service,
				repository,
				100,
				1,
				7
		);
	}

	private Safe182AmberAlertView alert() {
		return new Safe182AmberAlertView(
				"20260518",
				"파란색 상의",
				"12",
				"11",
				"010",
				"남자",
				"서울특별시 종로구",
				"홍길동",
				"145",
				"38",
				"보통",
				"계란형",
				"짧은머리",
				"흑색",
				"1234",
				"자료 출처: 경찰청"
		);
	}

	private static class RecordingMissingAlertService extends MissingAlertService {
		private final Safe182AmberAlertPage response;
		private final List<LocalDate> requestedDates = new ArrayList<>();

		RecordingMissingAlertService(Safe182AmberAlertPage response) {
			super((occurrenceDate, rowSize, page) -> response);
			this.response = response;
		}

		@Override
		public Safe182AmberAlertPage fetchAlerts(int rowSize, Integer page, LocalDate occurrenceDate) {
			requestedDates.add(occurrenceDate);
			return response;
		}
	}

	private static class RecordingMissingPersonRepository {
		private final boolean existsByExternalId;
		private final boolean existsBySourceType;
		private final List<MissingPersonReport> savedReports = new ArrayList<>();

		RecordingMissingPersonRepository(boolean existsByExternalId, boolean existsBySourceType) {
			this.existsByExternalId = existsByExternalId;
			this.existsBySourceType = existsBySourceType;
		}

		MissingPersonRepository proxy() {
			return (MissingPersonRepository) Proxy.newProxyInstance(
					MissingPersonRepository.class.getClassLoader(),
					new Class<?>[] { MissingPersonRepository.class },
					(proxy, method, args) -> switch (method.getName()) {
						case "existsByApiProviderAndExternalId" -> existsByExternalId;
						case "existsBySourceType" -> existsBySourceType;
						case "save" -> {
							MissingPersonReport report = (MissingPersonReport) args[0];
							savedReports.add(report);
							yield report;
						}
						default -> throw new UnsupportedOperationException(method.getName());
					}
			);
		}
	}
}
