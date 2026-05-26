package com.example.dogo.controller.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;
import com.example.dogo.dto.missing.Safe182AmberAlertView;
import com.example.dogo.service.missing.MissingAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MissingAlertApiControllerTest {

	private final RecordingMissingAlertService missingAlertService = new RecordingMissingAlertService();
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new MissingAlertApiController(missingAlertService))
			.build();

	@Test
	void returnsMissingAlertsFromSafe182() throws Exception {
		missingAlertService.response = new Safe182AmberAlertPage("00", "OK", 1, List.of(alert()));

		mockMvc.perform(get("/api/missing-alerts")
						.param("rowSize", "5")
						.param("page", "2")
						.param("occrde", "20260518"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount", is(1)))
				.andExpect(jsonPath("$.alerts[0].name", is("홍길동")))
				.andExpect(jsonPath("$.alerts[0].sourceLabel", is("자료 출처: 경찰청")));

		org.assertj.core.api.Assertions.assertThat(missingAlertService.rowSize).isEqualTo(5);
		org.assertj.core.api.Assertions.assertThat(missingAlertService.page).isEqualTo(2);
		org.assertj.core.api.Assertions.assertThat(missingAlertService.occurrenceDate).isEqualTo(LocalDate.of(2026, 5, 18));
	}

	private Safe182AmberAlertView alert() {
		return new Safe182AmberAlertView(
				"20260518",
				"파란색 상의",
				"12",
				"11",
				"010",
				"남자",
				"서울특별시 종로구",
				"홍길동",
				"145",
				"38",
				"보통",
				"계란형",
				"짧은머리",
				"흑색",
				"1234",
				"자료 출처: 경찰청"
		);
	}

	private static class RecordingMissingAlertService extends MissingAlertService {
		private int rowSize;
		private Integer page;
		private LocalDate occurrenceDate;
		private Safe182AmberAlertPage response = new Safe182AmberAlertPage("00", "OK", 0, List.of());

		RecordingMissingAlertService() {
			super(null, null);
		}

		@Override
		public Safe182AmberAlertPage fetchAlerts(int rowSize, Integer page, LocalDate occurrenceDate) {
			this.rowSize = rowSize;
			this.page = page;
			this.occurrenceDate = occurrenceDate;
			return response;
		}
	}
}
