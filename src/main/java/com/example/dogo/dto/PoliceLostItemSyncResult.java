package com.example.dogo.dto;

public record PoliceLostItemSyncResult(
		int fetchedCount,
		int savedCount,
		int skippedCount,
		int pageCount
) {
}
