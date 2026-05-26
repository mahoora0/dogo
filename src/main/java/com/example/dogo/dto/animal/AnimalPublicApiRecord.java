package com.example.dogo.dto.animal;

public record AnimalPublicApiRecord(
		String externalId,
		String eventDate,
		String eventPlace,
		String regionName,
		String careTel,
		String kindName,
		String breedName,
		String color,
		String sexCode,
		String neuterCode,
		String ageText,
		String weightText,
		String feature,
		String processState,
		String imageUrl,
		String rawPayload
) {
}
