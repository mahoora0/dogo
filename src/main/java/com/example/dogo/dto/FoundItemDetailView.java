package com.example.dogo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FoundItemDetailView(
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
		String description,
		String contact,
		String colorName,
		List<String> imageUrls
) {
}
