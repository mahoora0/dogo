package com.example.dogo.service.missing.sync;

import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.service.missing.client.Safe182MissingPersonClient;
import com.example.dogo.service.missing.client.Safe182MissingPersonPage;
import com.example.dogo.service.missing.client.Safe182MissingPersonRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Safe182MissingPersonSyncServiceTest {

	private final Safe182MissingPersonClient client = mock(Safe182MissingPersonClient.class);
	private final MissingPersonRepository repository = mock(MissingPersonRepository.class);
	private final Safe182MissingPersonSyncService syncService = new Safe182MissingPersonSyncService(client, repository);

	@Test
	void importsNewSafe182SearchRecordsIntoMissingPersonReports() {
		Safe182MissingPersonRecord record = record();
		when(client.search("Hong", 1, 20)).thenReturn(new Safe182MissingPersonPage("00", "OK", 1, List.of(record)));
		when(repository.findByApiProviderAndExternalId("SAFE182_FIND_CHILD", "safe182-1")).thenReturn(Optional.empty());

		Safe182MissingPersonSyncResult result = syncService.syncSearch("Hong");

		assertThat(result.imported()).isEqualTo(1);
		verify(repository).save(any(MissingPersonReport.class));
	}

	@Test
	void skipsAlreadyImportedSafe182Records() {
		Safe182MissingPersonRecord record = record();
		when(client.search("Hong", 1, 20)).thenReturn(new Safe182MissingPersonPage("00", "OK", 1, List.of(record)));
		when(repository.findByApiProviderAndExternalId("SAFE182_FIND_CHILD", "safe182-1"))
				.thenReturn(Optional.of(MissingPersonReport.fromPublicApi(
						"SAFE182_FIND_CHILD",
						"safe182-1",
						"{}",
						13,
						"Unknown",
						LocalDateTime.of(2026, 5, 18, 0, 0),
						"Seoul Gangnam",
						170,
						new BigDecimal("58.0"),
						"Slim",
						"Oval",
						"Black",
						"Short",
						"Blue hoodie"
				)));

		Safe182MissingPersonSyncResult result = syncService.syncSearch("Hong");

		assertThat(result.skipped()).isEqualTo(1);
		verify(repository, never()).save(any(MissingPersonReport.class));
		verify(client).search(eq("Hong"), eq(1), eq(20));
	}

	private Safe182MissingPersonRecord record() {
		return new Safe182MissingPersonRecord(
				"safe182-1",
				"Hong Gil-dong",
				"male",
				13,
				LocalDateTime.of(2026, 5, 18, 0, 0),
				"Seoul Gangnam",
				170,
				new BigDecimal("58.0"),
				"Slim",
				"Oval",
				"Black",
				"Short",
				"Blue hoodie",
				"{\"nm\":\"Hong Gil-dong\"}"
		);
	}
}
