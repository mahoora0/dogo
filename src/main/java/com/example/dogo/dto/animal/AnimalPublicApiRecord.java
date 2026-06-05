package com.example.dogo.dto.animal;

public record AnimalPublicApiRecord(
		String externalId,
		String eventDate,
		String eventPlace,
		String regionName,
		String careName,
		String orgName,
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
		String noticeNo,
		String noticeStartDate,
		String noticeEndDate,
		String imageUrl,
		String rawPayload
) {
}
