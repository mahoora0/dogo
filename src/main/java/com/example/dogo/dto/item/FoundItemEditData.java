package com.example.dogo.dto.item;

import java.time.LocalDateTime;
import java.util.List;

public record FoundItemEditData(
		Long id,
		String title,
		String itemName,
		String categoryMain,
		String categorySub,
		String colorName,
		LocalDateTime foundAt,
		String foundAreaProvince,
		String foundAreaDistrict,
		String foundPlace,
		String keepPlace,
		String content,
		List<String> existingImageUrls
) {}
