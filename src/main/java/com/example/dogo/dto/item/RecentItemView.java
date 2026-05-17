package com.example.dogo.dto.item;

import java.time.LocalDateTime;

public record RecentItemView(
		Long id,
		String type,
		String typeLabel,
		String title,
		String category,
		String place,
		LocalDateTime itemAt,
		String status,
		String statusLabel,
		String imageUrl
) {
}
