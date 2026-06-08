package com.example.dogo.controller.item;

import com.example.dogo.dto.item.LostItemCreateRequest;
import com.example.dogo.controller.common.ListBackUrlBuilder;
import com.example.dogo.service.item.LostItemService;
import com.example.dogo.service.item.RegistrationOptionService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.dogo.security.CustomUserDetails;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LostItemController {

	private static final int MAX_PAGE_SIZE = 30;

	private final LostItemService lostItemService;
	private final RegistrationOptionService registrationOptionService;

	@ModelAttribute("categories")
	public List<String> categories() {
		return registrationOptionService.getCategoryMainOptions();
	}

	@ModelAttribute("categorySubOptions")
	public Map<String, List<String>> categorySubOptions() {
		return registrationOptionService.getCategorySubOptions();
	}

	@ModelAttribute("colorOptions")
	public List<String> colorOptions() {
		return registrationOptionService.getColorOptions();
	}

	@ModelAttribute("regionOptions")
	public List<String> regionOptions() {
		return registrationOptionService.getRegionOptions();
	}

	@ModelAttribute("regionDistrictOptions")
	public Map<String, List<String>> regionDistrictOptions() {
		return registrationOptionService.getRegionDistrictOptions();
	}

	@ModelAttribute("keywordScopeOptions")
	public List<Option> keywordScopeOptions() {
		return List.of(
				new Option("ALL", "전체"),
				new Option("TITLE_PLACE", "제목+장소"),
				new Option("ITEM_CATEGORY", "물품명+분류"),
				new Option("CONTENT", "상세내용"),
				new Option("COLOR", "색상")
		);
	}

	private static final java.util.Set<String> LOST_SORT_FIELDS = java.util.Set.of("regDate", "lostAt");

	@GetMapping("/lost-items")
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "ALL") String keywordScope,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String area,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
			@RequestParam(required = false) String detailPlace,
			@RequestParam(defaultValue = "regDate") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "9") int size,
			Model model
	) {
		String safeField = LOST_SORT_FIELDS.contains(sortBy) ? sortBy : "regDate";
		Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
		Sort sort = Sort.by(direction, safeField).and(Sort.by(Sort.Direction.DESC, "lostId"));

		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		PageRequest pageRequest = PageRequest.of(safePage, safeSize, sort);
		String safeKeywordScope = keywordScopeOptions().stream()
				.map(Option::value)
				.filter(value -> value.equals(keywordScope))
				.findFirst()
				.orElse("ALL");
		Page<?> lostItemPage;
		if (startDate == null && (detailPlace == null || detailPlace.trim().isEmpty())) {
			lostItemPage = lostItemService.search(keyword, safeKeywordScope, category, area, status, pageRequest);
		} else {
			lostItemPage = lostItemService.search(keyword, safeKeywordScope, category, area, status, startDate, detailPlace, pageRequest);
		}
		if (safePage > 0 && safePage >= lostItemPage.getTotalPages() && lostItemPage.getTotalPages() > 0) {
			safePage = lostItemPage.getTotalPages() - 1;
			if (startDate == null && (detailPlace == null || detailPlace.trim().isEmpty())) {
				lostItemPage = lostItemService.search(keyword, safeKeywordScope, category, area, status, PageRequest.of(safePage, safeSize, sort));
			} else {
				lostItemPage = lostItemService.search(keyword, safeKeywordScope, category, area, status, startDate, detailPlace, PageRequest.of(safePage, safeSize, sort));
			}
		}

		model.addAttribute("lostItemPage", lostItemPage);
		model.addAttribute("lostItems", lostItemPage.getContent());
		model.addAttribute("searchCategories", lostItemService.getSearchCategoryNames());
		model.addAttribute("keyword", keyword);
		model.addAttribute("keywordScope", safeKeywordScope);
		model.addAttribute("category", category);
		model.addAttribute("area", area);
		model.addAttribute("status", status);
		model.addAttribute("startDate", startDate);
		model.addAttribute("detailPlace", detailPlace);
		model.addAttribute("sortBy", safeField);
		model.addAttribute("sortDir", sortDir);
		model.addAttribute("page", safePage);
		model.addAttribute("size", safeSize);
		model.addAttribute("currentUri", "/lost-items");
		return "lost-items/list";
	}

	@GetMapping("/lost-items/new")
	public String createForm(Model model) {
		model.addAttribute("currentUri", "/lost-items/new");
		return "lost-items/new";
	}

	@PostMapping("/lost-items")
	public String create(@ModelAttribute("request") LostItemCreateRequest request,
						 @AuthenticationPrincipal CustomUserDetails userDetails,
						 Model model) {
		try {
			Long lostItemId = lostItemService.create(request, userDetails != null ? userDetails.getUser() : null);
			return "redirect:/lost-items/" + lostItemId + "?created=true";
		} catch (IllegalArgumentException exception) {
			model.addAttribute("errorMessage", exception.getMessage());
			return "lost-items/new";
		} catch (Exception exception) {
			model.addAttribute("errorMessage", "등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
			return "lost-items/new";
		}
	}

	@GetMapping("/lost-items/{id}")
	public String detail(@PathVariable Long id,
						 @RequestParam(defaultValue = "false") boolean created,
						 @RequestParam(defaultValue = "false") boolean rematching,
						 @RequestParam(required = false) String keyword,
						 @RequestParam(required = false) String keywordScope,
						 @RequestParam(required = false) String category,
						 @RequestParam(required = false) String area,
						 @RequestParam(required = false) String status,
						 @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
						 @RequestParam(required = false) String detailPlace,
						 @RequestParam(required = false) String sortBy,
						 @RequestParam(required = false) String sortDir,
						 @RequestParam(required = false) Integer page,
						 @RequestParam(required = false) Integer size,
						 @AuthenticationPrincipal CustomUserDetails userDetails,
						 Model model) {
		var lostItem = lostItemService.getDetail(id);
		var matchCandidates = lostItemService.getMatchCandidates(id);
		Long currentUserNo = userDetails != null ? userDetails.getUser().getUserNo() : null;
		boolean matchingInProgress = (created || rematching) && matchCandidates.isEmpty();
		model.addAttribute("lostItem", lostItem);
		model.addAttribute("matchCandidates", matchCandidates);
		model.addAttribute("matchingInProgress", matchingInProgress);
		model.addAttribute("isOwner", currentUserNo != null && currentUserNo.equals(lostItem.userNo()));
		model.addAttribute("listUrl", ListBackUrlBuilder.fromPath("/lost-items")
				.queryParam("keyword", keyword)
				.queryParam("keywordScope", keywordScope)
				.queryParam("category", category)
				.queryParam("area", area)
				.queryParam("status", status)
				.queryParam("startDate", startDate)
				.queryParam("detailPlace", detailPlace)
				.queryParam("sortBy", sortBy)
				.queryParam("sortDir", sortDir)
				.queryParam("page", page)
				.queryParam("size", size)
				.build());
		return "lost-items/detail";
	}

	@GetMapping("/lost-items/{id}/edit")
	public String editForm(@PathVariable Long id,
						   @AuthenticationPrincipal CustomUserDetails userDetails,
						   Model model) {
		if (userDetails == null) {
			return "redirect:/login";
		}
		try {
			model.addAttribute("editData", lostItemService.getForEdit(id, userDetails.getUser()));
			return "lost-items/edit";
		} catch (IllegalArgumentException e) {
			model.addAttribute("message", e.getMessage());
			return "lost-items/error";
		}
	}

	@PostMapping("/lost-items/{id}/edit")
	public String edit(@PathVariable Long id,
					   @ModelAttribute("request") LostItemCreateRequest request,
					   @AuthenticationPrincipal CustomUserDetails userDetails,
					   Model model) {
		if (userDetails == null) {
			return "redirect:/login";
		}
		try {
			lostItemService.update(id, request, userDetails.getUser());
			return "redirect:/lost-items/" + id + "?rematching=true";
		} catch (IllegalArgumentException e) {
			try {
				model.addAttribute("editData", lostItemService.getForEdit(id, userDetails.getUser()));
			} catch (Exception ignored) {
			}
			model.addAttribute("errorMessage", e.getMessage());
			return "lost-items/edit";
		}
	}

	@PostMapping("/lost-items/{id}/status")
	public String updateStatus(@PathVariable Long id,
							   @RequestParam String status,
							   @AuthenticationPrincipal CustomUserDetails userDetails,
							   RedirectAttributes redirectAttributes) {
		if (userDetails == null) {
			return "redirect:/login";
		}
		try {
			lostItemService.updateStatus(id, status, userDetails.getUser());
			redirectAttributes.addFlashAttribute("statusSuccessMessage", "게시글 상태가 변경되었습니다.");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("statusErrorMessage", e.getMessage());
		}
		return "redirect:/lost-items/" + id;
	}

	@PostMapping("/lost-items/{id}/delete")
	public String delete(@PathVariable Long id,
						 @AuthenticationPrincipal CustomUserDetails userDetails,
						 Model model) {
		if (userDetails == null) {
			return "redirect:/login";
		}
		try {
			lostItemService.delete(id, userDetails.getUser());
			return "redirect:/lost-items";
		} catch (IllegalArgumentException e) {
			model.addAttribute("message", e.getMessage());
			return "lost-items/error";
		}
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String notFound(IllegalArgumentException exception, Model model) {
		model.addAttribute("message", exception.getMessage());
		return "lost-items/error";
	}

	public record Option(String value, String label) {
	}
}
