package com.example.dogo.controller;

import com.example.dogo.dto.ReportCreateRequest;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.report.PostReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PostReportController {

	private final PostReportService postReportService;

	public PostReportController(PostReportService postReportService) {
		this.postReportService = postReportService;
	}

	@PostMapping("/reports")
	public String create(
			@ModelAttribute ReportCreateRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			RedirectAttributes redirectAttributes
	) {
		if (userDetails == null) {
			return "redirect:/login";
		}

		String redirectUrl = postReportService.fallbackTargetUrl(request.getTargetType(), request.getTargetId());
		try {
			redirectUrl = postReportService.create(request, userDetails.getUser());
			redirectAttributes.addFlashAttribute("reportSuccessMessage", "신고가 접수되었습니다.");
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("reportErrorMessage", exception.getMessage());
		}
		return "redirect:" + redirectUrl;
	}
}
