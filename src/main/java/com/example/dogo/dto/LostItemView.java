package com.example.dogo.dto;

import java.time.LocalDate;

public record LostItemView(
		Long id,
		String name,
		String category,
		String area,
		String place,
		LocalDate lostDate,
		String status,
		String description,
		String imageUrl
) {
}
