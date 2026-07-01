package com.example.dogo.service.police.sync;

import com.example.dogo.service.missing.MissingAlertSyncRunner;
import com.example.dogo.service.animal.api.AnimalLossApiSyncRunner;
import com.example.dogo.service.animal.api.AnimalProtectionApiSyncRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PoliceItemStartupBackfillRunnerTest {

	@Test
	void backfillOnStartupSubmitsLostAndFoundBackfills() {
		RecordingPoliceLostItemSyncRunner lostItemSyncRunner = new RecordingPoliceLostItemSyncRunner();
		RecordingPoliceFoundItemSyncRunner foundItemSyncRunner = new RecordingPoliceFoundItemSyncRunner();
		RecordingMissingAlertSyncRunner missingAlertSyncRunner = new RecordingMissingAlertSyncRunner();
		RecordingAnimalLossApiSyncRunner animalLossApiSyncRunner = new RecordingAnimalLossApiSyncRunner();
		RecordingAnimalProtectionApiSyncRunner animalProtectionApiSyncRunner = new RecordingAnimalProtectionApiSyncRunner();
		PoliceItemStartupBackfillRunner runner = new PoliceItemStartupBackfillRunner(
				lostItemSyncRunner,
				foundItemSyncRunner,
				missingAlertSyncRunner,
				animalLossApiSyncRunner,
				animalProtectionApiSyncRunner,
				Runnable::run
		);

		runner.backfillOnStartupIfEmpty();

		assertThat(lostItemSyncRunner.backfillCount).isEqualTo(1);
		assertThat(foundItemSyncRunner.backfillCount).isEqualTo(1);
		assertThat(missingAlertSyncRunner.backfillCount).isEqualTo(1);
		assertThat(animalLossApiSyncRunner.backfillCount).isEqualTo(1);
		assertThat(animalProtectionApiSyncRunner.backfillCount).isEqualTo(1);
	}

	private static class RecordingPoliceLostItemSyncRunner extends PoliceLostItemSyncRunner {
		private int backfillCount;

		RecordingPoliceLostItemSyncRunner() {
			super(null, null, false, false, false);
		}

		@Override
		public void backfillOnStartupIfEmpty() {
			backfillCount++;
		}
	}

	private static class RecordingPoliceFoundItemSyncRunner extends PoliceFoundItemSyncRunner {
		private int backfillCount;

		RecordingPoliceFoundItemSyncRunner() {
			super(null, null, false, false, false);
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

	private static class RecordingAnimalLossApiSyncRunner extends AnimalLossApiSyncRunner {
		private int backfillCount;

		RecordingAnimalLossApiSyncRunner() {
			super(null, null, null, null, null, 100, 7, 30, false, false);
		}

		@Override
		public void backfillOnStartupIfEmpty() {
			backfillCount++;
		}
	}

	private static class RecordingAnimalProtectionApiSyncRunner extends AnimalProtectionApiSyncRunner {
		private int backfillCount;

		RecordingAnimalProtectionApiSyncRunner() {
			super(null, null, null, null, null, 100, 7, 30, false, false);
		}

		@Override
		public void backfillOnStartupIfEmpty() {
			backfillCount++;
		}
	}
}
