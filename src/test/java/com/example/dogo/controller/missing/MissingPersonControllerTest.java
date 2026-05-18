package com.example.dogo.controller.missing;

import com.example.dogo.dto.missing.MissingPersonCreateRequest;
import com.example.dogo.dto.missing.MissingPersonDetailView;
import com.example.dogo.dto.missing.MissingPersonView;
import com.example.dogo.service.missing.MissingPersonService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MissingPersonControllerTest {

	private final MissingPersonService missingPersonService = mock(MissingPersonService.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new MissingPersonController(missingPersonService))
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.build();

	@Test
	void listShowsMissingPersonBoard() throws Exception {
		MissingPersonView report = new MissingPersonView(
				1L,
				"13세 대한민국 실종자",
				13,
				"대한민국",
				LocalDateTime.of(2026, 5, 18, 9, 30),
				"서울 강남역",
				170,
				new BigDecimal("58.0"),
				"마른 체형",
				"계란형",
				"검정",
				"짧은 머리",
				"흰색 후드티",
				"OPEN",
				"접수"
		);
		when(missingPersonService.search(eq("대한민국"), eq("OPEN"), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(report)));

		mockMvc.perform(get("/missing-persons").param("keyword", "대한민국").param("status", "OPEN"))
				.andExpect(status().isOk())
				.andExpect(view().name("missing-persons/list"))
				.andExpect(model().attribute("currentUri", "/missing-persons"))
				.andExpect(model().attributeExists("reports"))
				.andExpect(model().attribute("keyword", "대한민국"))
				.andExpect(model().attribute("status", "OPEN"));
	}

	@Test
	void createFormShowsMissingPersonForm() throws Exception {
		mockMvc.perform(get("/missing-persons/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("missing-persons/new"))
				.andExpect(model().attribute("currentUri", "/missing-persons/new"))
				.andExpect(model().attributeExists("request"));
	}

	@Test
	void createRedirectsToDetail() throws Exception {
		when(missingPersonService.create(any(MissingPersonCreateRequest.class), any())).thenReturn(12L);

		mockMvc.perform(post("/missing-persons")
						.param("age", "13")
						.param("nationality", "대한민국")
						.param("occurredAt", "2026-05-18T09:30")
						.param("occurredPlace", "서울 강남역")
						.param("heightCm", "170")
						.param("weightKg", "58.0")
						.param("bodyType", "마른 체형")
						.param("faceShape", "계란형")
						.param("hairColor", "검정")
						.param("hairStyle", "짧은 머리")
						.param("clothing", "흰색 후드티"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/missing-persons/12?created=true"));
	}

	@Test
	void detailShowsMissingPersonReport() throws Exception {
		when(missingPersonService.getDetail(12L)).thenReturn(new MissingPersonDetailView(
				12L,
				"13세 대한민국 실종자",
				13,
				"대한민국",
				LocalDateTime.of(2026, 5, 18, 9, 30),
				"서울 강남역",
				170,
				new BigDecimal("58.0"),
				"마른 체형",
				"계란형",
				"검정",
				"짧은 머리",
				"흰색 후드티",
				"OPEN",
				"접수"
		));

		mockMvc.perform(get("/missing-persons/12"))
				.andExpect(status().isOk())
				.andExpect(view().name("missing-persons/detail"))
				.andExpect(model().attributeExists("report"))
				.andExpect(model().attribute("currentUri", "/missing-persons"));
	}
}
