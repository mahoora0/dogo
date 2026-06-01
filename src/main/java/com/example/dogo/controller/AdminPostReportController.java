package com.example.dogo.controller;

import com.example.dogo.entity.ReportReasonType;
import com.example.dogo.entity.ReportStatus;
import com.example.dogo.entity.ReportTargetType;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.report.PostReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reports")
public class AdminPostReportController {

	private static final int PAGE_SIZE = 20;

	private final PostReportService postReportService;

	public AdminPostReportController(PostReportService postReportService) {
		this.postReportService = postReportService;
	}

	@GetMapping
	public String list(
			@RequestParam(required = false) ReportStatus status,
			@RequestParam(required = false) ReportTargetType targetType,
			@RequestParam(required = false) ReportReasonType reasonType,
			@RequestParam(defaultValue = "0") int page,
			Model model
	) {
		int safePage = Math.max(page, 0);
		var pageable = PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "status").and(Sort.by(Sort.Direction.DESC, "createdAt")));
		var reportPage = postReportService.search(status, targetType, reasonType, pageable);

		model.addAttribute("reportPage", reportPage);
		model.addAttribute("reports", reportPage.getContent());
		model.addAttribute("status", status);
		model.addAttribute("targetType", targetType);
		model.addAttribute("reasonType", reasonType);
		model.addAttribute("statuses", ReportStatus.values());
		model.addAttribute("targetTypes", ReportTargetType.values());
		model.addAttribute("reasonTypes", ReportReasonType.values());
		model.addAttribute("page", safePage);
		return "admin/reports";
	}

	@PostMapping("/{id}/status")
	public String updateStatus(
			@PathVariable Long id,
			@RequestParam ReportStatus status,
			@RequestParam(required = false) String adminMemo,
			@RequestParam(required = false) ReportStatus filterStatus,
			@RequestParam(required = false) ReportTargetType targetType,
			@RequestParam(required = false) ReportReasonType reasonType,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			RedirectAttributes redirectAttributes
	) {
		try {
			postReportService.updateStatus(id, status, adminMemo, userDetails != null ? userDetails.getUser() : null);
			redirectAttributes.addFlashAttribute("adminReportMessage", "신고 상태가 변경되었습니다.");
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("adminReportError", exception.getMessage());
		}

		if (filterStatus != null) {
			redirectAttributes.addAttribute("status", filterStatus);
		}
		if (targetType != null) {
			redirectAttributes.addAttribute("targetType", targetType);
		}
		if (reasonType != null) {
			redirectAttributes.addAttribute("reasonType", reasonType);
		}
		return "redirect:/admin/reports";
	}
}
