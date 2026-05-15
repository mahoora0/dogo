package com.example.dogo.service.match.semantic;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;

public record SemanticMatchItem(
		long id,
		String type,
		String itemName,
		String title,
		String category,
		String color,
		String area,
		String place,
		String content
) {

	public static SemanticMatchItem fromLost(LostItem item) {
		return new SemanticMatchItem(
				item.getLostId(),
				"LOST",
				item.getItemName(),
				item.getTitle(),
				item.getCategoryMain(),
				item.getColorName(),
				item.getLostArea(),
				item.getLostPlace(),
				item.getContent()
		);
	}

	public static SemanticMatchItem fromFound(FoundItem item) {
		return new SemanticMatchItem(
				item.getFoundId(),
				"FOUND",
				item.getItemName(),
				item.getTitle(),
				item.getCategoryMain(),
				item.getColorName(),
				item.getFoundArea(),
				item.getFoundPlace(),
				item.getContent()
		);
	}
}
