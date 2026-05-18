package com.example.dogo.dto.item;

import java.time.LocalDateTime;
import java.util.List;

public record LostItemEditData(
		Long id,
		String title,
		String itemName,
		String categoryMain,
		String categorySub,
		String colorName,
		LocalDateTime lostAt,
		String lostAreaProvince,
		String lostAreaDistrict,
		String lostPlace,
		String contact,
		String content,
		List<String> existingImageUrls
) {}
