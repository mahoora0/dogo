package com.example.dogo.service.police.sync;

import com.example.dogo.service.missing.MissingAlertSyncRunner;
import com.example.dogo.service.animal.api.AnimalLossApiSyncRunner;
import com.example.dogo.service.animal.api.AnimalProtectionApiSyncRunner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class PoliceItemStartupBackfillRunner {

	private final PoliceLostItemSyncRunner lostItemSyncRunner;
	private final PoliceFoundItemSyncRunner foundItemSyncRunner;
	private final MissingAlertSyncRunner missingAlertSyncRunner;
	private final AnimalLossApiSyncRunner animalLossApiSyncRunner;
	private final AnimalProtectionApiSyncRunner animalProtectionApiSyncRunner;
	private final TaskExecutor backfillExecutor;

	public PoliceItemStartupBackfillRunner(
			PoliceLostItemSyncRunner lostItemSyncRunner,
			PoliceFoundItemSyncRunner foundItemSyncRunner,
			MissingAlertSyncRunner missingAlertSyncRunner,
			AnimalLossApiSyncRunner animalLossApiSyncRunner,
			AnimalProtectionApiSyncRunner animalProtectionApiSyncRunner,
			@Qualifier("policeStartupBackfillExecutor") TaskExecutor backfillExecutor
	) {
		this.lostItemSyncRunner = lostItemSyncRunner;
		this.foundItemSyncRunner = foundItemSyncRunner;
		this.missingAlertSyncRunner = missingAlertSyncRunner;
		this.animalLossApiSyncRunner = animalLossApiSyncRunner;
		this.animalProtectionApiSyncRunner = animalProtectionApiSyncRunner;
		this.backfillExecutor = backfillExecutor;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void backfillOnStartupIfEmpty() {
		backfillExecutor.execute(lostItemSyncRunner::backfillOnStartupIfEmpty);
		backfillExecutor.execute(foundItemSyncRunner::backfillOnStartupIfEmpty);
		backfillExecutor.execute(missingAlertSyncRunner::backfillOnStartupIfEmpty);
		backfillExecutor.execute(animalLossApiSyncRunner::backfillOnStartupIfEmpty);
		backfillExecutor.execute(animalProtectionApiSyncRunner::backfillOnStartupIfEmpty);
	}
}
