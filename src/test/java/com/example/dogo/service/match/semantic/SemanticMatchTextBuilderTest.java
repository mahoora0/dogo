package com.example.dogo.service.match.semantic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticMatchTextBuilderTest {

	private final SemanticMatchTextBuilder builder = new SemanticMatchTextBuilder();

	@Test
	void buildIncludesItemNameTitleCategoryAndColor() {
		SemanticMatchItem item = new SemanticMatchItem(
				1L,
				"LOST",
				"검정 카드지갑",
				"강남역 카드지갑 분실",
				"지갑",
				"검정",
				"서울특별시 강남구",
				"강남역",
				"상세 설명은 semantic v2 텍스트에서 제외"
		);

		String text = builder.build(item);

		assertThat(text).isEqualTo("물품명: 검정 카드지갑. 제목: 강남역 카드지갑 분실. 카테고리: 지갑. 색상: 검정");
	}

	@Test
	void hashChangesWhenTextVersionChangesInput() {
		String text = "물품명: 검정 카드지갑";

		assertThat(builder.hash(text)).isEqualTo(builder.hash(text));
		assertThat(builder.hash(text)).isNotEqualTo(builder.hash(text + ". 색상: 검정"));
	}

	@Test
	void buildReturnsEmptyForBlankFields() {
		SemanticMatchItem item = new SemanticMatchItem(
				1L, "FOUND", " ", null, "", null, null, null, null);

		assertThat(builder.build(item)).isEmpty();
	}
}
