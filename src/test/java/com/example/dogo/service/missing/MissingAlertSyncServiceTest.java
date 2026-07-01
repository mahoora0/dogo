package com.example.dogo.service.missing;

import com.example.dogo.service.missing.client.Safe182MissingPersonClient;
import com.example.dogo.service.missing.client.Safe182MissingPersonPage;
import com.example.dogo.service.missing.client.Safe182MissingPersonRecord;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.missing.MissingPersonRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissingAlertSyncServiceTest {

	@Test
	void syncDateStoresPublicApiMissingReports() {
		RecordingMissingPersonClient client = new RecordingMissingPersonClient(
				new Safe182MissingPersonPage("00", "OK", 1, List.of(record()))
		);
		RecordingMissingPersonRepository repository = new RecordingMissingPersonRepository(false, false);
		MissingAlertSyncService syncService = syncService(client, repository.proxy());

		MissingAlertSyncResult result = syncService.syncDate(LocalDate.of(2026, 5, 18));

		assertThat(result.fetchedCount()).isEqualTo(1);
		assertThat(result.savedCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isZero();
		assertThat(repository.savedReports).hasSize(1);
		assertThat(repository.savedReports.get(0).getSourceType()).isEqualTo("PUBLIC_API");
		assertThat(repository.savedReports.get(0).getApiProvider()).isEqualTo("SAFE182_MISSING_PERSON");
		assertThat(repository.savedReports.get(0).getPersonName()).isEqualTo("홍길동");
		assertThat(repository.savedReports.get(0).getGender()).isEqualTo("남자");
	}

	@Test
	void syncDateSkipsExistingPublicApiReports() {
		RecordingMissingPersonClient client = new RecordingMissingPersonClient(
				new Safe182MissingPersonPage("00", "OK", 1, List.of(record()))
		);
		RecordingMissingPersonRepository repository = new RecordingMissingPersonRepository(true, false);
		MissingAlertSyncService syncService = syncService(client, repository.proxy());

		MissingAlertSyncResult result = syncService.syncDate(LocalDate.of(2026, 5, 18));

		assertThat(result.fetchedCount()).isEqualTo(1);
		assertThat(result.savedCount()).isZero();
		assertThat(result.skippedCount()).isEqualTo(1);
		assertThat(repository.savedReports).isEmpty();
	}

	@Test
	void syncAllFetchesAllActiveRecordsWithoutDateFilter() {
		RecordingMissingPersonClient client = new RecordingMissingPersonClient(
				new Safe182MissingPersonPage("00", "OK", 0, List.of())
		);
		RecordingMissingPersonRepository repository = new RecordingMissingPersonRepository(false, false);
		MissingAlertSyncService syncService = syncService(client, repository.proxy());

		MissingAlertSyncResult result = syncService.syncAll();

		assertThat(result.pageCount()).isEqualTo(1);
		assertThat(client.searchAllCalled).isTrue();
		assertThat(client.requestedDates).isEmpty();
	}

	private MissingAlertSyncService syncService(Safe182MissingPersonClient client, MissingPersonRepository repository) {
		return new MissingAlertSyncService(
				client,
				repository,
				100,
				1,
				7
		);
	}

	private Safe182MissingPersonRecord record() {
		return new Safe182MissingPersonRecord(
				"12345",
				"홍길동",
				"남자",
				10,
				LocalDateTime.of(2026, 5, 18, 0, 0),
				"서울특별시 종로구",
				145,
				new BigDecimal("38.0"),
				"보통",
				"계란형",
				"흑색",
				"짧은머리",
				"파란색 상의",
				"자료 출처: 경찰청"
		);
	}

	private static class RecordingMissingPersonClient implements Safe182MissingPersonClient {
		private final Safe182MissingPersonPage response;
		private final List<LocalDate> requestedDates = new ArrayList<>();
		private boolean searchAllCalled = false;

		RecordingMissingPersonClient(Safe182MissingPersonPage response) {
			this.response = response;
		}

		@Override
		public Safe182MissingPersonPage search(String keyword, int page, int rowSize) {
			searchAllCalled = true;
			return response;
		}

		@Override
		public Safe182MissingPersonPage searchByDateRange(LocalDate startDate, LocalDate endDate, int page, int rowSize) {
			requestedDates.add(startDate); // Assumes we just track the startDate for verification
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
