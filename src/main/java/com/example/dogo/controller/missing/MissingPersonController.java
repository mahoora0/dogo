package com.example.dogo.controller.missing;

import com.example.dogo.dto.missing.MissingPersonCreateRequest;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.missing.MissingPersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequiredArgsConstructor
public class MissingPersonController {

	private static final int MAX_PAGE_SIZE = 30;
	private static final java.util.Set<String> SORT_FIELDS = java.util.Set.of("regdate", "occurredAt");

	private final MissingPersonService missingPersonService;

	@GetMapping("/missing-persons")
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String sourceType,
			@RequestParam(defaultValue = "occurredAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "9") int size,
			Model model
	) {
		String safeField = SORT_FIELDS.contains(sortBy) ? sortBy : "regdate";
		Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
		Sort sort = Sort.by(direction, safeField).and(Sort.by(Sort.Direction.DESC, "reportId"));
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

		Page<?> reportPage = missingPersonService.search(keyword, status, sourceType, PageRequest.of(safePage, safeSize, sort));
		if (safePage > 0 && safePage >= reportPage.getTotalPages() && reportPage.getTotalPages() > 0) {
			safePage = reportPage.getTotalPages() - 1;
			reportPage = missingPersonService.search(keyword, status, sourceType, PageRequest.of(safePage, safeSize, sort));
		}

		model.addAttribute("reportPage", reportPage);
		model.addAttribute("reports", reportPage.getContent());
		model.addAttribute("keyword", keyword);
		model.addAttribute("status", status);
		model.addAttribute("sourceType", sourceType);
		model.addAttribute("sortBy", safeField);
		model.addAttribute("sortDir", sortDir);
		model.addAttribute("page", safePage);
		model.addAttribute("size", safeSize);
		model.addAttribute("currentUri", "/missing-persons");
		return "missing-persons/list";
	}

	@GetMapping("/missing-persons/new")
	public String createForm(Model model) {
		model.addAttribute("request", new MissingPersonCreateRequest());
		model.addAttribute("currentUri", "/missing-persons/new");
		return "missing-persons/new";
	}

	@PostMapping("/missing-persons")
	public String create(
			@ModelAttribute("request") MissingPersonCreateRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Model model
	) {
		try {
			Long reportId = missingPersonService.create(request, userDetails != null ? userDetails.getUser() : null);
			return "redirect:/missing-persons/" + reportId + "?created=true";
		} catch (IllegalArgumentException exception) {
			model.addAttribute("errorMessage", exception.getMessage());
			model.addAttribute("currentUri", "/missing-persons/new");
			return "missing-persons/new";
		}
	}

	@GetMapping("/missing-persons/{id}")
	public String detail(@PathVariable Long id,
						 @AuthenticationPrincipal CustomUserDetails userDetails,
						 Model model) {
		var report = missingPersonService.getDetail(id);
		Long currentUserNo = userDetails != null ? userDetails.getUser().getUserNo() : null;
		model.addAttribute("report", report);
		model.addAttribute("isOwner", currentUserNo != null && currentUserNo.equals(report.userNo()));
		model.addAttribute("currentUri", "/missing-persons");
		return "missing-persons/detail";
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String notFound(IllegalArgumentException exception, Model model) {
		model.addAttribute("message", exception.getMessage());
		return "missing-persons/error";
	}
}
