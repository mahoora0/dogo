package com.example.dogo.controller;

import com.example.dogo.dto.LostItemCreateRequest;
import com.example.dogo.service.CategoryService;
import com.example.dogo.service.LostItemService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class LostItemController {

	private static final int MAX_PAGE_SIZE = 30;

	private final CategoryService categoryService;
	private final LostItemService lostItemService;

	@ModelAttribute("categories")
	public List<String> categories() {
		return categoryService.getActiveCategoryNames();
	}

	@GetMapping("/")
	public String home() {
		return "redirect:/lost-items";
	}

	@GetMapping("/lost-items")
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
				Sort.by(Sort.Direction.DESC, "lostAt").and(Sort.by(Sort.Direction.DESC, "lostId"))
		);
		Page<?> lostItemPage = lostItemService.search(keyword, category, area, status, pageRequest);
		if (safePage > 0 && safePage >= lostItemPage.getTotalPages() && lostItemPage.getTotalPages() > 0) {
			safePage = lostItemPage.getTotalPages() - 1;
			pageRequest = PageRequest.of(
					safePage,
					safeSize,
					Sort.by(Sort.Direction.DESC, "lostAt").and(Sort.by(Sort.Direction.DESC, "lostId"))
			);
			lostItemPage = lostItemService.search(keyword, category, area, status, pageRequest);
		}

		model.addAttribute("lostItemPage", lostItemPage);
		model.addAttribute("lostItems", lostItemPage.getContent());
		model.addAttribute("searchCategories", lostItemService.getSearchCategoryNames());
		model.addAttribute("keyword", keyword);
		model.addAttribute("category", category);
		model.addAttribute("area", area);
		model.addAttribute("status", status);
		model.addAttribute("page", safePage);
		model.addAttribute("size", safeSize);
		return "lost-items/list";
	}

	@GetMapping("/lost-items/new")
	public String createForm() {
		return "lost-items/new";
	}

	@PostMapping("/lost-items")
	public String create(@ModelAttribute("request") LostItemCreateRequest request, Model model) {
		try {
			Long lostItemId = lostItemService.create(request);
			return "redirect:/lost-items/" + lostItemId;
		} catch (IllegalArgumentException exception) {
			model.addAttribute("errorMessage", exception.getMessage());
			return "lost-items/new";
		} catch (Exception exception) {
			model.addAttribute("errorMessage", "등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
			return "lost-items/new";
		}
	}

	@GetMapping("/lost-items/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("lostItem", lostItemService.getDetail(id));
		return "lost-items/detail";
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String notFound(IllegalArgumentException exception, Model model) {
		model.addAttribute("message", exception.getMessage());
		return "lost-items/error";
	}
}
