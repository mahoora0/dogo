package com.example.dogo.service.police.sync;

import com.example.dogo.service.missing.MissingAlertSyncRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PoliceItemStartupBackfillRunnerTest {

	@Test
	void backfillOnStartupSubmitsLostAndFoundBackfills() {
		RecordingPoliceLostItemSyncRunner lostItemSyncRunner = new RecordingPoliceLostItemSyncRunner();
		RecordingPoliceFoundItemSyncRunner foundItemSyncRunner = new RecordingPoliceFoundItemSyncRunner();
		RecordingMissingAlertSyncRunner missingAlertSyncRunner = new RecordingMissingAlertSyncRunner();
		PoliceItemStartupBackfillRunner runner = new PoliceItemStartupBackfillRunner(
				lostItemSyncRunner,
				foundItemSyncRunner,
				missingAlertSyncRunner,
				Runnable::run
		);

		runner.backfillOnStartupIfEmpty();

		assertThat(lostItemSyncRunner.backfillCount).isEqualTo(1);
		assertThat(foundItemSyncRunner.backfillCount).isEqualTo(1);
		assertThat(missingAlertSyncRunner.backfillCount).isEqualTo(1);
	}

	private static class RecordingPoliceLostItemSyncRunner extends PoliceLostItemSyncRunner {
		private int backfillCount;

		RecordingPoliceLostItemSyncRunner() {
			super(null, null, false, false);
		}

		@Override
		public void backfillOnStartupIfEmpty() {
			backfillCount++;
		}
	}

	private static class RecordingPoliceFoundItemSyncRunner extends PoliceFoundItemSyncRunner {
		private int backfillCount;

		RecordingPoliceFoundItemSyncRunner() {
			super(null, null, false, false);
		}

		@Override
		public void backfillOnStartupIfEmpty() {
			backfillCount++;
		}
	}

	private static class RecordingMissingAlertSyncRunner extends MissingAlertSyncRunner {
		private int backfillCount;

		RecordingMissingAlertSyncRunner() {
			super(null, null, false, false);
		}

		@Override
		public void backfillOnStartupIfEmpty() {
			backfillCount++;
		}
	}
}
