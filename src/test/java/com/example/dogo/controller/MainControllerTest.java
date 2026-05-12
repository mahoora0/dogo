package com.example.dogo.controller;

import com.example.dogo.service.LostItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MainControllerTest {

	private final LostItemService lostItemService = mock(LostItemService.class);
	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MainController(lostItemService)).build();

	@Test
	void homeShowsIndexPage() throws Exception {
		when(lostItemService.getSearchCategoryNames()).thenReturn(List.of("가방", "전자기기"));

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(model().attribute("currentUri", "/"))
				.andExpect(model().attribute("searchCategories", List.of("가방", "전자기기")));
	}
}
