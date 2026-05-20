package com.example.dogo.controller.home;

import com.example.dogo.service.item.FoundItemService;
import com.example.dogo.service.item.LostItemService;
import com.example.dogo.service.match.ItemMatchService;
import com.example.dogo.service.missing.MissingPersonService;
import com.example.dogo.service.animal.AnimalReportService;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
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
	private final FoundItemService foundItemService = mock(FoundItemService.class);
	private final ItemMatchService itemMatchService = mock(ItemMatchService.class);
	private final MissingPersonService missingPersonService = mock(MissingPersonService.class);
	private final AnimalReportService animalReportService = mock(AnimalReportService.class);
	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new MainController(lostItemService, foundItemService, itemMatchService, missingPersonService, animalReportService))
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.build();

	@Test
	void homeShowsIndexPage() throws Exception {
		when(lostItemService.getSearchCategoryNames()).thenReturn(List.of("가방", "전자기기"));
		when(lostItemService.getRecentItems(8)).thenReturn(List.of());
		when(foundItemService.getRecentItems(8)).thenReturn(List.of());
		when(missingPersonService.getRecentItems(8)).thenReturn(List.of());
		when(animalReportService.getRecentItems(8)).thenReturn(List.of());

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(model().attribute("currentUri", "/"))
				.andExpect(model().attribute("searchCategories", List.of("가방", "전자기기")))
				.andExpect(model().attribute("recentItems", List.of()));
	}
}

