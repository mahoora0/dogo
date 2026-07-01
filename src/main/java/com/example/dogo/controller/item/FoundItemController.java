package com.example.dogo.controller.item;

import com.example.dogo.dto.item.FoundItemCreateRequest;
import com.example.dogo.controller.common.ListBackUrlBuilder;
import com.example.dogo.service.item.FoundItemService;
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
public class FoundItemController {

	private static final int MAX_PAGE_SIZE = 30;

	private final FoundItemService foundItemService;
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

	private static final java.util.Set<String> FOUND_SORT_FIELDS = java.util.Set.of("regDate", "foundAt");

	@GetMapping("/found-items")
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "ALL") String keywordScope,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String area,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
			@RequestParam(required = false) String detailPlace,
			@RequestParam(defaultValue = "foundAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size,
			Model model
	) {
		String safeField = FOUND_SORT_FIELDS.contains(sortBy) ? sortBy : "foundAt";
		Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
		Sort sort = Sort.by(direction, safeField).and(Sort.by(Sort.Direction.DESC, "foundId"));

		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		PageRequest pageRequest = PageRequest.of(safePage, safeSize, sort);
		String safeKeywordScope = keywordScopeOptions().stream()
				.map(Option::value)
				.filter(value -> value.equals(keywordScope))
				.findFirst()
				.orElse("ALL");
		Page<?> foundItemPage;
		if (startDate == null && (detailPlace == null || detailPlace.trim().isEmpty())) {
			foundItemPage = foundItemService.search(keyword, safeKeywordScope, category, area, status, pageRequest);
		} else {
			foundItemPage = foundItemService.search(keyword, safeKeywordScope, category, area, status, startDate, detailPlace, pageRequest);
		}
		if (safePage > 0 && safePage >= foundItemPage.getTotalPages() && foundItemPage.getTotalPages() > 0) {
			safePage = foundItemPage.getTotalPages() - 1;
			if (startDate == null && (detailPlace == null || detailPlace.trim().isEmpty())) {
				foundItemPage = foundItemService.search(keyword, safeKeywordScope, category, area, status, PageRequest.of(safePage, safeSize, sort));
			} else {
				foundItemPage = foundItemService.search(keyword, safeKeywordScope, category, area, status, startDate, detailPlace, PageRequest.of(safePage, safeSize, sort));
			}
		}

		model.addAttribute("foundItemPage", foundItemPage);
		model.addAttribute("foundItems", foundItemPage.getContent());
		model.addAttribute("searchCategories", foundItemService.getSearchCategoryNames());
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
			return "redirect:/found-items/" + foundItemId + "?created=true";
		} catch (IllegalArgumentException exception) {
			model.addAttribute("errorMessage", exception.getMessage());
			return "found-items/new";
		} catch (Exception exception) {
			model.addAttribute("errorMessage", "등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
			return "found-items/new";
		}
	}

	@GetMapping("/found-items/{id}")
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
		var foundItem = foundItemService.getDetail(id);
		var matchCandidates = foundItemService.getMatchCandidates(id);
		Long currentUserNo = userDetails != null ? userDetails.getUser().getUserNo() : null;
		boolean matchingInProgress = (created || rematching) && matchCandidates.isEmpty();
		model.addAttribute("foundItem", foundItem);
		model.addAttribute("matchCandidates", matchCandidates);
		model.addAttribute("matchingInProgress", matchingInProgress);
		model.addAttribute("isOwner", currentUserNo != null && currentUserNo.equals(foundItem.userNo()));
		model.addAttribute("listUrl", ListBackUrlBuilder.fromPath("/found-items")
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
		return "found-items/detail";
	}

	@GetMapping("/found-items/{id}/edit")
	public String editForm(@PathVariable Long id,
						   @AuthenticationPrincipal CustomUserDetails userDetails,
						   Model model) {
		if (userDetails == null) {
			return "redirect:/login";
		}
		try {
			model.addAttribute("editData", foundItemService.getForEdit(id, userDetails.getUser()));
			return "found-items/edit";
		} catch (IllegalArgumentException e) {
			model.addAttribute("message", e.getMessage());
			return "found-items/error";
		}
	}

	@PostMapping("/found-items/{id}/edit")
	public String edit(@PathVariable Long id,
					   @ModelAttribute("request") FoundItemCreateRequest request,
					   @AuthenticationPrincipal CustomUserDetails userDetails,
					   Model model) {
		if (userDetails == null) {
			return "redirect:/login";
		}
		try {
			foundItemService.update(id, request, userDetails.getUser());
			return "redirect:/found-items/" + id + "?rematching=true";
		} catch (IllegalArgumentException e) {
			try {
				model.addAttribute("editData", foundItemService.getForEdit(id, userDetails.getUser()));
			} catch (Exception ignored) {
			}
			model.addAttribute("errorMessage", e.getMessage());
			return "found-items/edit";
		}
	}

	@PostMapping("/found-items/{id}/status")
	public String updateStatus(@PathVariable Long id,
							   @RequestParam String status,
							   @AuthenticationPrincipal CustomUserDetails userDetails,
							   RedirectAttributes redirectAttributes) {
		if (userDetails == null) {
			return "redirect:/login";
		}
		try {
			foundItemService.updateStatus(id, status, userDetails.getUser());
			redirectAttributes.addFlashAttribute("statusSuccessMessage", "게시글 상태가 변경되었습니다.");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("statusErrorMessage", e.getMessage());
		}
		return "redirect:/found-items/" + id;
	}

	@PostMapping("/found-items/{id}/delete")
	public String delete(@PathVariable Long id,
						 @AuthenticationPrincipal CustomUserDetails userDetails,
						 Model model) {
		if (userDetails == null) {
			return "redirect:/login";
		}
		try {
			foundItemService.delete(id, userDetails.getUser());
			return "redirect:/found-items";
		} catch (IllegalArgumentException e) {
			model.addAttribute("message", e.getMessage());
			return "found-items/error";
		}
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String notFound(IllegalArgumentException exception, Model model) {
		model.addAttribute("message", exception.getMessage());
		return "found-items/error";
	}

	public record Option(String value, String label) {
	}
}
