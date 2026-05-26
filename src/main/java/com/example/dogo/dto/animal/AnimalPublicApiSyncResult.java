package com.example.dogo.dto.animal;

public record AnimalPublicApiSyncResult(
		int fetchedCount,
		int savedCount,
		int skippedCount,
		int pageCount
) {
}
