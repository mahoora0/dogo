package com.example.dogo.controller.item;

import com.example.dogo.dto.item.LostItemCreateRequest;
import com.example.dogo.dto.item.LostItemDetailView;
import com.example.dogo.dto.item.LostItemView;
import com.example.dogo.service.item.LostItemService;
import com.example.dogo.service.item.RegistrationOptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
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

class LostItemControllerTest {

	private LostItemService lostItemService;
	private RegistrationOptionService registrationOptionService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		lostItemService = mock(LostItemService.class);
		registrationOptionService = new RegistrationOptionService();
		when(lostItemService.getSearchCategoryNames()).thenReturn(List.of("가방", "전자기기"));
		mockMvc = MockMvcBuilders.standaloneSetup(new LostItemController(lostItemService, registrationOptionService))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
	}

	@Test
	void listAddsSearchResultAndFiltersToModel() throws Exception {
		LostItemView view = new LostItemView(
				1L,
				"검정 지갑을 찾습니다",
				"검정 지갑",
				"지갑",
				"서울",
				"강남역",
				LocalDateTime.of(2026, 5, 8, 12, 0),
				"WAITING",
				"대기중",
				"블랙(검정)",
				"카드가 들어있습니다",
				"010-1234-5678",
				"/uploads/lost-items/wallet.jpg"
		);
		when(lostItemService.search(eq("지갑"), eq("ITEM_CATEGORY"), eq("지갑"), eq("강남"), eq("WAITING"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(view)));

		mockMvc.perform(get("/lost-items")
							.param("keyword", "지갑")
							.param("keywordScope", "ITEM_CATEGORY")
							.param("category", "지갑")
						.param("area", "강남")
						.param("status", "WAITING"))
				.andExpect(status().isOk())
				.andExpect(view().name("lost-items/list"))
					.andExpect(model().attribute("lostItems", hasSize(1)))
					.andExpect(model().attribute("keyword", "지갑"))
					.andExpect(model().attribute("keywordScope", "ITEM_CATEGORY"))
					.andExpect(model().attribute("category", "지갑"))
				.andExpect(model().attribute("area", "강남"))
				.andExpect(model().attribute("status", "WAITING"))
				.andExpect(model().attribute("categories", registrationOptionService.getCategoryMainOptions()))
				.andExpect(model().attribute("searchCategories", List.of("가방", "전자기기")));

		verify(lostItemService).search(eq("지갑"), eq("ITEM_CATEGORY"), eq("지갑"), eq("강남"), eq("WAITING"), any(Pageable.class));
		verify(lostItemService).getSearchCategoryNames();
	}

	@Test
	void createRedirectsToCreatedLostItemDetail() throws Exception {
		when(lostItemService.create(any(LostItemCreateRequest.class), isNull())).thenReturn(7L);

		mockMvc.perform(post("/lost-items")
						.param("title", "검정 지갑을 찾습니다")
						.param("itemName", "검정 지갑")
						.param("categoryMain", "지갑")
						.param("lostAt", "2026-05-08T12:00")
						.param("lostArea", "서울")
						.param("lostPlace", "강남역")
						.param("contact", "010-1234-5678")
						.param("content", "카드가 들어있습니다"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/lost-items/7?created=true"));

		verify(lostItemService).create(any(LostItemCreateRequest.class), isNull());
	}

	@Test
	void detailAddsLostItemToModel() throws Exception {
		LostItemDetailView detail = new LostItemDetailView(
				7L,
				"검정 지갑을 찾습니다",
				"검정 지갑",
				"지갑",
				"서울",
				"강남역",
				LocalDateTime.of(2026, 5, 8, 12, 0),
				"WAITING",
				"대기중",
				"블랙(검정)",
				"카드가 들어있습니다",
				"010-1234-5678",
				List.of("/uploads/lost-items/wallet.jpg")
		);
		when(lostItemService.getDetail(7L)).thenReturn(detail);

		mockMvc.perform(get("/lost-items/7"))
				.andExpect(status().isOk())
				.andExpect(view().name("lost-items/detail"))
				.andExpect(model().attribute("lostItem", detail));

		verify(lostItemService).getDetail(7L);
	}
}
