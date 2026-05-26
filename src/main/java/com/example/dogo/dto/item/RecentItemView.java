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
		String imageUrl,
		LocalDateTime regDate
) {
	// Overloaded constructor for 10-parameter compatibility
	public RecentItemView(
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
		this(id, type, typeLabel, title, category, place, itemAt, status, statusLabel, imageUrl, itemAt);
	}
}
