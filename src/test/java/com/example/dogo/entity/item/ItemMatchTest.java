package com.example.dogo.entity.item;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemMatchTest {

	@Test
	void storesMatchReasonsAsJsonArray() {
		ItemMatch match = new ItemMatch(
				null,
				null,
				new BigDecimal("55.00"),
				null,
				new BigDecimal("55.00"),
				List.of("시간 근접", "색상 일치"),
				"java-rule-v1",
				null
		);

		assertThat(match.getMatchReasons()).isEqualTo("[\"시간 근접\",\"색상 일치\"]");
		assertThat(match.getMatchReasonList()).containsExactly("시간 근접", "색상 일치");
	}

	@Test
	void readsLegacyNewlineSeparatedMatchReasons() {
		ItemMatch match = new ItemMatch(null, null, new BigDecimal("55.00"));
		ReflectionTestUtils.setField(match, "matchReasons", "시간 근접\r\n색상 일치\n장소 일치");

		assertThat(match.getMatchReasonList()).containsExactly("시간 근접", "색상 일치", "장소 일치");
	}
}
