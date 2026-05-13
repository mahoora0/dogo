package com.example.dogo.controller.item;

import com.example.dogo.dto.item.FoundItemCreateRequest;
import com.example.dogo.dto.item.FoundItemDetailView;
import com.example.dogo.dto.item.FoundItemView;
import com.example.dogo.entity.user.User;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.item.FoundItemService;
import com.example.dogo.service.item.RegistrationOptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class FoundItemControllerTest {

	private FoundItemService foundItemService;
	private RegistrationOptionService registrationOptionService;
	private FoundItemController foundItemController;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		foundItemService = mock(FoundItemService.class);
		registrationOptionService = new RegistrationOptionService();
		foundItemController = new FoundItemController(foundItemService, registrationOptionService);
		when(foundItemService.getSearchCategoryNames()).thenReturn(List.of("가방", "전자기기"));
		mockMvc = MockMvcBuilders.standaloneSetup(foundItemController)
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
	}

	@Test
	void listAddsSearchResultAndFiltersToModel() throws Exception {
		FoundItemView view = new FoundItemView(
				1L,
				"검정 지갑을 주웠습니다",
				"검정 지갑",
				"지갑",
				"서울",
				"강남역",
				"강남경찰서",
				LocalDateTime.of(2026, 5, 8, 12, 0),
				"KEEPING",
				"보관중",
				"검정",
				"/uploads/found-items/wallet.jpg"
		);
		when(foundItemService.search(eq("지갑"), eq("지갑"), eq("강남"), eq("KEEPING"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(view)));

		mockMvc.perform(get("/found-items")
						.param("keyword", "지갑")
						.param("category", "지갑")
						.param("area", "강남")
						.param("status", "KEEPING"))
				.andExpect(status().isOk())
				.andExpect(view().name("found-items/list"))
				.andExpect(model().attribute("foundItems", hasSize(1)))
				.andExpect(model().attribute("keyword", "지갑"))
				.andExpect(model().attribute("category", "지갑"))
				.andExpect(model().attribute("area", "강남"))
				.andExpect(model().attribute("status", "KEEPING"))
				.andExpect(model().attribute("categories", registrationOptionService.getCategoryMainOptions()))
				.andExpect(model().attribute("searchCategories", List.of("가방", "전자기기")));

		verify(foundItemService).search(eq("지갑"), eq("지갑"), eq("강남"), eq("KEEPING"), any(Pageable.class));
		verify(foundItemService).getSearchCategoryNames();
	}

	@Test
	void createRedirectsToCreatedFoundItemDetailWhenAnonymous() throws Exception {
		when(foundItemService.create(any(FoundItemCreateRequest.class), isNull())).thenReturn(7L);

		mockMvc.perform(post("/found-items")
						.param("title", "검정 지갑을 주웠습니다")
						.param("itemName", "검정 지갑")
						.param("categoryMain", "지갑")
						.param("foundAt", "2026-05-08T12:00")
						.param("foundArea", "서울")
						.param("foundPlace", "강남역")
						.param("keepPlace", "강남경찰서")
						.param("colorName", "검정")
						.param("content", "카드가 들어 있습니다"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/found-items/7"));

		verify(foundItemService).create(any(FoundItemCreateRequest.class), isNull());
	}

	@Test
	void createPassesAuthenticatedUserToService() {
		User user = new User("login@dogo.local", "로그인 사용자", "010-9999-8888");
		when(foundItemService.create(any(FoundItemCreateRequest.class), eq(user))).thenReturn(8L);

		Model model = new ExtendedModelMap();
		String viewName = foundItemController.create(new FoundItemCreateRequest(), new CustomUserDetails(user), model);

		verify(foundItemService).create(any(FoundItemCreateRequest.class), eq(user));
		org.assertj.core.api.Assertions.assertThat(viewName).isEqualTo("redirect:/found-items/8");
	}

	@Test
	void detailAddsFoundItemToModel() throws Exception {
		FoundItemDetailView detail = new FoundItemDetailView(
				7L,
				"검정 지갑을 주웠습니다",
				"검정 지갑",
				"지갑",
				"서울",
				"강남역",
				"강남경찰서",
				LocalDateTime.of(2026, 5, 8, 12, 0),
				"KEEPING",
				"보관중",
				"카드가 들어 있습니다",
				"서울역(한국철도공사) / 02-3149-2531",
				"검정",
				List.of("/uploads/found-items/wallet.jpg")
		);
		when(foundItemService.getDetail(7L)).thenReturn(detail);

		mockMvc.perform(get("/found-items/7"))
				.andExpect(status().isOk())
				.andExpect(view().name("found-items/detail"))
				.andExpect(model().attribute("foundItem", detail));

		verify(foundItemService).getDetail(7L);
	}
}
