package com.example.dogo.service.missing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MissingAlertSyncResultTest {

	@Test
	void exposesSyncCounters() {
		MissingAlertSyncResult result = new MissingAlertSyncResult(3, 2, 1, 4);

		assertThat(result.fetchedCount()).isEqualTo(3);
		assertThat(result.savedCount()).isEqualTo(2);
		assertThat(result.skippedCount()).isEqualTo(1);
		assertThat(result.pageCount()).isEqualTo(4);
	}
}
