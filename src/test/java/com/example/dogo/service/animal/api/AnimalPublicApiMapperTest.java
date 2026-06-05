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
				"장성군동물보호센터",
				"전라남도 장성군",
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
				"전남-장성-2026-00014",
				"20260518",
				"20260528",
				null,
				"<item><desertionNo>000114</desertionNo></item>"
		), "ANIMAL_PROTECTION_API", "SIGHTING");

		assertThat(report.getTitle()).isEqualTo("구조 동물 000114");
		assertThat(report.getBreedName()).isNull();
		assertThat(report.getContent()).contains(
				"상태: 종료(반환)",
				"나이: 2022(년생)",
				"체중: 10(Kg)"
		);
		assertThat(report.getContent()).doesNotContain("state:", "feature:", "age:", "weight:", "보호소:", "공고번호:");
	}

	@Test
	void parsesCommaDecimalWeightFromPublicApi() {
		AnimalReport report = mapper.toReport(new AnimalPublicApiRecord(
				"PROTECT-3",
				"20260605",
				"원통",
				"강원특별자치도 인제군 인제읍 덕산로 256-41",
				"인제군동물보호센터",
				"강원특별자치도 인제군",
				"033-460-2473",
				"[고양이] 한국 고양이",
				null,
				"레몬색&흰색",
				"Q",
				"N",
				"2026(60일미만)(년생)",
				"0,36(Kg)",
				"새끼 고양이",
				"보호중",
				"강원-인제-2026-00173",
				"20260604",
				"20260604",
				null,
				"<item/>"
		), "ANIMAL_PROTECTION_API", "SIGHTING");

		assertThat(report.getWeightKg()).isEqualByComparingTo("0.36");
		assertThat(report.getContent()).contains("상태: 보호중", "체중: 0,36(Kg)");
		assertThat(report.getContent()).doesNotContain("보호소:", "공고번호:", "공고기간:");
	}

	@Test
	void kindNameDeterminesAnimalTypeAndBreedWhenHumanReadable() {
		AnimalReport report = mapper.toReport(new AnimalPublicApiRecord(
				"PROTECT-2",
				"20260518",
				"Mapo",
				"Seoul",
				null,
				null,
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
