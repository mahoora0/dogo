package com.example.dogo.controller;

import com.example.dogo.dto.LostItemCreateRequest;
import com.example.dogo.service.LostItemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LostItemController {

	private final LostItemService lostItemService;

	public LostItemController(LostItemService lostItemService) {
		this.lostItemService = lostItemService;
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
			Model model
	) {
		model.addAttribute("lostItems", lostItemService.search(keyword, category, area, status));
		model.addAttribute("keyword", keyword);
		model.addAttribute("category", category);
		model.addAttribute("area", area);
		model.addAttribute("status", status);
		return "lost-items/list";
	}

	@GetMapping("/lost-items/new")
	public String createForm() {
		return "lost-items/new";
	}

	@PostMapping("/lost-items")
	public String create(@ModelAttribute LostItemCreateRequest request) {
		Long lostItemId = lostItemService.create(request);
		return "redirect:/lost-items/" + lostItemId;
	}

	@GetMapping("/lost-items/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("lostItem", lostItemService.getDetail(id));
		return "lost-items/detail";
	}
}
