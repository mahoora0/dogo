package com.example.dogo.service;

import com.example.dogo.entity.FoundItem;
import com.example.dogo.entity.LostItem;
import com.example.dogo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ItemMatchScorerTest {

	private ItemMatchScorer scorer;

	@BeforeEach
	void setUp() {
		MatchTextNormalizer normalizer = new MatchTextNormalizer();
		scorer = new ItemMatchScorer(normalizer, new MatchTextTokenizer(normalizer));
	}

	@Test
	void scoreRejectsFoundItemBeforeLostMargin() {
		LostItem lost = lostItem("검정 지갑", "지갑", LocalDateTime.of(2026, 5, 10, 12, 0), "서울", "강남역");
		FoundItem found = foundItem("검정 지갑", "지갑", LocalDateTime.of(2026, 5, 6, 11, 0), "서울", "강남역", "검정");

		MatchScoreResult result = scorer.score(lost, found);

		assertThat(result.eligible()).isFalse();
		assertThat(result.reasons()).contains("분실/습득 시간이 매칭 범위를 벗어났습니다.");
	}

	@Test
	void scoreDoesNotPassWithCategoryOnly() {
		LostItem lost = lostItem("지갑", "지갑", LocalDateTime.of(2026, 5, 1, 12, 0), null, "장소 미상");
		FoundItem found = foundItem("지갑", "지갑", LocalDateTime.of(2026, 6, 10, 12, 0), null, "다른 장소", null);

		MatchScoreResult result = scorer.score(lost, found);

		assertThat(result.eligible()).isTrue();
		assertThat(result.totalScore()).isLessThan(MatchTestNumbers.MIN_STORE_SCORE);
	}

	@Test
	void scoreUsesColorSynonymsAndItemExpansions() {
		LostItem lost = lostItem(
				"검정색루에브르지갑",
				"지갑",
				LocalDateTime.of(2026, 5, 10, 18, 0),
				"서울",
				"강남역 2번출구"
		);
		FoundItem found = foundItem(
				"블랙 여성 반지갑",
				"지갑",
				LocalDateTime.of(2026, 5, 10, 20, 0),
				"서울",
				"강남역",
				"블랙"
		);

		MatchScoreResult result = scorer.score(lost, found);

		assertThat(result.eligible()).isTrue();
		assertThat(result.totalScore()).isGreaterThanOrEqualTo(MatchTestNumbers.MIN_STORE_SCORE);
		assertThat(result.reasons()).anyMatch(reason -> reason.contains("색상 일치 (검정)"));
		assertThat(result.reasons()).anyMatch(reason -> reason.contains("물품 키워드 일치"));
	}

	@Test
	void scoreKeepsTransportLocationCandidateWhenPlacesAreDifferent() {
		LostItem lost = lostItem("검정 지갑", "지갑", LocalDateTime.of(2026, 5, 10, 8, 0), "서울", "버스에서 분실");
		FoundItem found = foundItem("검정 지갑", "지갑", LocalDateTime.of(2026, 5, 10, 10, 0), "경기", "광명경찰서", "검정");

		MatchScoreResult result = scorer.score(lost, found);

		assertThat(result.eligible()).isTrue();
		assertThat(result.locationScore()).isGreaterThanOrEqualTo(new BigDecimal("6.00"));
		assertThat(result.reasons()).contains("이동수단 분실 가능성");
	}

	@Test
	void scoreRejectsCompletedStatus() {
		LostItem lost = lostItem("검정 지갑", "지갑", LocalDateTime.of(2026, 5, 10, 12, 0), "서울", "강남역");
		FoundItem found = foundItem("검정 지갑", "지갑", LocalDateTime.of(2026, 5, 10, 13, 0), "서울", "강남역", "검정");
		ReflectionTestUtils.setField(found, "status", "RETURNED");

		MatchScoreResult result = scorer.score(lost, found);

		assertThat(result.eligible()).isFalse();
		assertThat(result.reasons()).contains("매칭 대상 상태가 아닙니다.");
	}

	private LostItem lostItem(String itemName, String category, LocalDateTime lostAt, String area, String place) {
		return new LostItem(
				new User("lost@dogo.local", "분실자", "010-1111-2222"),
				itemName,
				null,
					itemName,
					category,
					null,
					null,
					lostAt,
				area,
				place,
				"010-0000-0000"
		);
	}

	private FoundItem foundItem(String itemName, String category, LocalDateTime foundAt, String area, String place, String color) {
		return new FoundItem(
				new User("found@dogo.local", "습득자", "010-3333-4444"),
				itemName,
				itemName,
				category,
				null,
				foundAt,
				area,
				place,
				"경찰서",
				color,
				null,
				"02-0000-0000"
		);
	}

	private static final class MatchTestNumbers {
		private static final java.math.BigDecimal MIN_STORE_SCORE = new java.math.BigDecimal("45.00");
	}
}
