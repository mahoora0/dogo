package com.example.dogo.dto.animal;

import java.util.List;

public record AnimalPublicApiPage(
		String resultCode,
		String resultMessage,
		int totalCount,
		List<AnimalPublicApiRecord> records
) {
}
