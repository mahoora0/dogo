package com.example.dogo.dto;

import java.util.List;

public record PoliceLostItemPage(
		String resultCode,
		String resultMessage,
		int totalCount,
		List<PoliceLostItemResponse> items
) {
	public PoliceLostItemPage {
		items = List.copyOf(items);
	}
}
