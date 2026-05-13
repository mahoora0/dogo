package com.example.dogo.dto.item;

import java.time.LocalDateTime;

public record FoundItemView(
		Long id,
		String title,
		String name,
		String category,
		String area,
		String place,
		String keepPlace,
		LocalDateTime foundAt,
		String status,
		String statusLabel,
		String colorName,
		String imageUrl
) {
}
