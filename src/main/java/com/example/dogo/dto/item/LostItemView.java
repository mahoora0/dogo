package com.example.dogo.dto.item;

import java.time.LocalDateTime;

public record LostItemView(
		Long id,
		String title,
		String name,
		String category,
		String area,
		String place,
		LocalDateTime lostAt,
		String status,
		String statusLabel,
		String colorName,
		String description,
		String contact,
		String imageUrl
) {
}
