package com.example.dogo.dto;

public record PoliceFoundItemSyncResult(
		int fetchedCount,
		int savedCount,
		int skippedCount,
		int pageCount
) {
}
