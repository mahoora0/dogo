package com.example.dogo.dto.police;

public record PoliceLostItemSyncResult(
		int fetchedCount,
		int savedCount,
		int skippedCount,
		int pageCount
) {
}
