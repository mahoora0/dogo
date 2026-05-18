package com.example.dogo.service.missing;

public record MissingAlertSyncResult(
		int fetchedCount,
		int savedCount,
		int skippedCount,
		int pageCount
) {

	public MissingAlertSyncResult plus(MissingAlertSyncResult other) {
		return new MissingAlertSyncResult(
				fetchedCount + other.fetchedCount,
				savedCount + other.savedCount,
				skippedCount + other.skippedCount,
				pageCount + other.pageCount
		);
	}
}
