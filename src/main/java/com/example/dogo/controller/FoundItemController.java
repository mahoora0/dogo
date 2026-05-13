package com.example.dogo.controller;

import com.example.dogo.dto.FoundItemCreateRequest;
import com.example.dogo.service.CategoryService;
import com.example.dogo.service.FoundItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.dogo.security.CustomUserDetails;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class FoundItemController {

	private static final int MAX_PAGE_SIZE = 30;

	private final CategoryService categoryService;
	private final FoundItemService foundItemService;

	@ModelAttribute("categories")
	public List<String> categories() {
		return categoryService.getActiveCategoryNames();
	}

	@GetMapping("/found-items")
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String area,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "9") int size,
			Model model
	) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		PageRequest pageRequest = PageRequest.of(
				safePage,
				safeSize,
				Sort.by(Sort.Direction.DESC, "foundAt").and(Sort.by(Sort.Direction.DESC, "foundId"))
		);
		Page<?> foundItemPage = foundItemService.search(keyword, category, area, status, pageRequest);
		if (safePage > 0 && safePage >= foundItemPage.getTotalPages() && foundItemPage.getTotalPages() > 0) {
			safePage = foundItemPage.getTotalPages() - 1;
			pageRequest = PageRequest.of(
					safePage,
					safeSize,
					Sort.by(Sort.Direction.DESC, "foundAt").and(Sort.by(Sort.Direction.DESC, "foundId"))
			);
			foundItemPage = foundItemService.search(keyword, category, area, status, pageRequest);
		}

		model.addAttribute("foundItemPage", foundItemPage);
		model.addAttribute("foundItems", foundItemPage.getContent());
		model.addAttribute("searchCategories", foundItemService.getSearchCategoryNames());
		model.addAttribute("keyword", keyword);
		model.addAttribute("category", category);
		model.addAttribute("area", area);
		model.addAttribute("status", status);
		model.addAttribute("page", safePage);
		model.addAttribute("size", safeSize);
		model.addAttribute("currentUri", "/found-items");
		return "found-items/list";
	}

	@GetMapping("/found-items/new")
	public String createForm(Model model) {
		model.addAttribute("currentUri", "/found-items/new");
		return "found-items/new";
	}

	@PostMapping("/found-items")
	public String create(@ModelAttribute("request") FoundItemCreateRequest request,
						 @AuthenticationPrincipal CustomUserDetails userDetails,
						 Model model) {
		try {
			Long foundItemId = foundItemService.create(request, userDetails != null ? userDetails.getUser() : null);
			return "redirect:/found-items/" + foundItemId;
		} catch (IllegalArgumentException exception) {
			model.addAttribute("errorMessage", exception.getMessage());
			return "found-items/new";
		} catch (Exception exception) {
			model.addAttribute("errorMessage", "등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
			return "found-items/new";
		}
	}

	@GetMapping("/found-items/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("foundItem", foundItemService.getDetail(id));
		return "found-items/detail";
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String notFound(IllegalArgumentException exception, Model model) {
		model.addAttribute("message", exception.getMessage());
		return "found-items/error";
	}
}
