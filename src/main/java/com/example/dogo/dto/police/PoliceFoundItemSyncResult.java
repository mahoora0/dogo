package com.example.dogo.dto.police;

public record PoliceFoundItemSyncResult(
		int fetchedCount,
		int savedCount,
		int skippedCount,
		int pageCount
) {
}
