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
import static org.mockito.Mockito.verify;
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
		MissingPersonView report = missingPersonView("USER", "사용자 제보");
		when(missingPersonService.search(eq("Korea"), eq("OPEN"), eq("PUBLIC_API"), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(report)));

		mockMvc.perform(get("/missing-persons")
						.param("keyword", "Korea")
						.param("status", "OPEN")
						.param("sourceType", "PUBLIC_API"))
				.andExpect(status().isOk())
				.andExpect(view().name("missing-persons/list"))
				.andExpect(model().attribute("currentUri", "/missing-persons"))
				.andExpect(model().attributeExists("reports"))
				.andExpect(model().attribute("keyword", "Korea"))
				.andExpect(model().attribute("status", "OPEN"))
				.andExpect(model().attribute("sourceType", "PUBLIC_API"));
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
						.param("nationality", "Korea")
						.param("occurredAt", "2026-05-18T09:30")
						.param("occurredPlace", "Seoul")
						.param("heightCm", "170")
						.param("weightKg", "58.0")
						.param("bodyType", "Slim")
						.param("faceShape", "Oval")
						.param("hairColor", "Black")
						.param("hairStyle", "Short")
						.param("clothing", "Blue hoodie"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/missing-persons/12?created=true"));
	}

	@Test
	void detailShowsMissingPersonReport() throws Exception {
		when(missingPersonService.getDetail(12L)).thenReturn(detail("PUBLIC_API", "공공데이터"));

		mockMvc.perform(get("/missing-persons/12"))
				.andExpect(status().isOk())
				.andExpect(view().name("missing-persons/detail"))
				.andExpect(model().attributeExists("report"))
				.andExpect(model().attribute("currentUri", "/missing-persons"));
	}

	private MissingPersonView missingPersonView(String sourceType, String sourceLabel) {
		return new MissingPersonView(
				1L,
				"13세 Korea 실종",
				13,
				"Korea",
				LocalDateTime.of(2026, 5, 18, 9, 30),
				"Seoul",
				170,
				new BigDecimal("58.0"),
				"Slim",
				"Oval",
				"Black",
				"Short",
				"Blue hoodie",
				"OPEN",
				"접수",
				sourceType,
				sourceLabel,
				null,
				java.util.List.of()
		);
	}

	private MissingPersonDetailView detail(String sourceType, String sourceLabel) {
		return new MissingPersonDetailView(
				12L,
				"13세 Korea 실종",
				13,
				"Korea",
				LocalDateTime.of(2026, 5, 18, 9, 30),
				"Seoul",
				170,
				new BigDecimal("58.0"),
				"Slim",
				"Oval",
				"Black",
				"Short",
				"Blue hoodie",
				"OPEN",
				"접수",
				sourceType,
				sourceLabel,
				null,
				15,
				"010",
				"정상아동(18세미만)",
				"남자",
				"특이사항 테스트",
				java.util.List.of(),
				99L
		);
	}
}
