package com.example.dogo.service.missing.client;

import java.util.List;

public record Safe182MissingPersonPage(
		String result,
		String message,
		int totalCount,
		List<Safe182MissingPersonRecord> records
) {
}
