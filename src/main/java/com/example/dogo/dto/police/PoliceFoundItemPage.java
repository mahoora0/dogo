package com.example.dogo.dto.police;

import java.util.List;

public record PoliceFoundItemPage(
		String resultCode,
		String resultMessage,
		int totalCount,
		List<PoliceFoundItemResponse> items
) {
	public PoliceFoundItemPage {
		items = List.copyOf(items);
	}
}
