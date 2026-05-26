package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiRecord;
import com.example.dogo.entity.animal.AnimalReport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnimalPublicApiMapperTest {

	private final AnimalPublicApiMapper mapper = new AnimalPublicApiMapper();

	@Test
	void rescuedAnimalTitleAndContentAreUserFacingKorean() {
		AnimalReport report = mapper.toReport(new AnimalPublicApiRecord(
				"000114",
				"20260518",
				"진원면 초동길16-6",
				"전라남도 장성군 삼계면 능성로 382-310",
				"061-390-8422",
				null,
				"000114",
				"크림색",
				"M",
				"U",
				"2022(년생)",
				"10(Kg)",
				"구조당시 빨간목걸이 착용,사람을 잘따르고 순한성격",
				"종료(반환)",
				null,
				"<item><desertionNo>000114</desertionNo></item>"
		), "ANIMAL_PROTECTION_API", "SIGHTING");

		assertThat(report.getTitle()).isEqualTo("구조 동물 000114");
		assertThat(report.getBreedName()).isNull();
		assertThat(report.getContent()).contains("상태: 종료(반환)", "나이: 2022(년생)", "체중: 10(Kg)");
		assertThat(report.getContent()).doesNotContain("state:", "feature:", "age:", "weight:");
	}

	@Test
	void kindNameDeterminesAnimalTypeAndBreedWhenHumanReadable() {
		AnimalReport report = mapper.toReport(new AnimalPublicApiRecord(
				"PROTECT-2",
				"20260518",
				"Mapo",
				"Seoul",
				null,
				"[개] 말티즈",
				null,
				"흰색",
				"F",
				"Y",
				null,
				null,
				null,
				null,
				null,
				"<item/>"
		), "ANIMAL_PROTECTION_API", "SIGHTING");

		assertThat(report.getTitle()).isEqualTo("말티즈 구조 신고");
		assertThat(report.getAnimalType()).isEqualTo("DOG");
		assertThat(report.getBreedName()).isEqualTo("말티즈");
	}
}
