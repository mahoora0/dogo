package com.example.dogo.service;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class PoliceItemStartupBackfillRunnerTest {

	@Test
	void backfillOnStartupSubmitsLostAndFoundBackfills() {
		PoliceLostItemSyncRunner lostItemSyncRunner = mock(PoliceLostItemSyncRunner.class);
		PoliceFoundItemSyncRunner foundItemSyncRunner = mock(PoliceFoundItemSyncRunner.class);
		PoliceItemStartupBackfillRunner runner = new PoliceItemStartupBackfillRunner(
				lostItemSyncRunner,
				foundItemSyncRunner,
				Runnable::run
		);

		runner.backfillOnStartupIfEmpty();

		InOrder inOrder = inOrder(lostItemSyncRunner, foundItemSyncRunner);
		inOrder.verify(lostItemSyncRunner).backfillOnStartupIfEmpty();
		inOrder.verify(foundItemSyncRunner).backfillOnStartupIfEmpty();
	}
}
