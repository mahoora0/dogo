package com.example.dogo.service;

import com.example.dogo.repository.LostItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoliceLostItemSyncRunnerTest {

	private PoliceLostItemSyncService syncService;
	private LostItemRepository lostItemRepository;

	@BeforeEach
	void setUp() {
		syncService = mock(PoliceLostItemSyncService.class);
		lostItemRepository = mock(LostItemRepository.class);
	}

	@Test
	void backfillOnStartupRunsWhenPoliceDataIsEmpty() {
		when(lostItemRepository.existsBySourceType("POLICE")).thenReturn(false);
		PoliceLostItemSyncRunner runner = new PoliceLostItemSyncRunner(
				syncService,
				lostItemRepository,
				true,
				true
		);

		runner.backfillOnStartupIfEmpty();

		verify(syncService).syncBackfillLastMonth();
	}

	@Test
	void backfillOnStartupSkipsWhenPoliceDataAlreadyExists() {
		when(lostItemRepository.existsBySourceType("POLICE")).thenReturn(true);
		PoliceLostItemSyncRunner runner = new PoliceLostItemSyncRunner(
				syncService,
				lostItemRepository,
				true,
				true
		);

		runner.backfillOnStartupIfEmpty();

		verify(syncService, never()).syncBackfillLastMonth();
	}

	@Test
	void backfillOnStartupSkipsWhenDisabled() {
		PoliceLostItemSyncRunner runner = new PoliceLostItemSyncRunner(
				syncService,
				lostItemRepository,
				true,
				false
		);

		runner.backfillOnStartupIfEmpty();

		verify(lostItemRepository, never()).existsBySourceType("POLICE");
		verify(syncService, never()).syncBackfillLastMonth();
	}

	@Test
	void scheduledSyncRunsIncrementalSyncWhenEnabled() {
		PoliceLostItemSyncRunner runner = new PoliceLostItemSyncRunner(
				syncService,
				lostItemRepository,
				true,
				false
		);

		runner.syncIncremental();

		verify(syncService).syncIncrementalLastMonth();
	}

	@Test
	void scheduledSyncSkipsWhenDisabled() {
		PoliceLostItemSyncRunner runner = new PoliceLostItemSyncRunner(
				syncService,
				lostItemRepository,
				false,
				true
		);

		runner.syncIncremental();

		verify(syncService, never()).syncIncrementalLastMonth();
	}
}
