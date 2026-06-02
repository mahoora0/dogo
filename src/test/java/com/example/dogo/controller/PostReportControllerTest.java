package com.example.dogo.controller;

import com.example.dogo.dto.ReportCreateRequest;
import com.example.dogo.entity.ReportReasonType;
import com.example.dogo.entity.ReportTargetType;
import com.example.dogo.entity.user.User;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.report.PostReportService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostReportControllerTest {

	private final PostReportService postReportService = mock(PostReportService.class);
	private final PostReportController controller = new PostReportController(postReportService);

	@Test
	void createRedirectsToSafeReturnUrlWhenProvidedFromChat() {
		User user = new User("reporter@example.com", "Reporter", "010-0000-0000");
		CustomUserDetails userDetails = new CustomUserDetails(user);
		RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
		ReportCreateRequest request = new ReportCreateRequest();
		request.setTargetType(ReportTargetType.FOUND_ITEM);
		request.setTargetId(7L);
		request.setReasonType(ReportReasonType.FRAUD);
		request.setReturnUrl("/chat?roomId=3");

		when(postReportService.fallbackTargetUrl(ReportTargetType.FOUND_ITEM, 7L)).thenReturn("/found-items/7");
		when(postReportService.create(request, user)).thenReturn("/found-items/7");

		String view = controller.create(request, userDetails, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/chat?roomId=3");
	}

	@Test
	void createIgnoresExternalReturnUrl() {
		User user = new User("reporter@example.com", "Reporter", "010-0000-0000");
		CustomUserDetails userDetails = new CustomUserDetails(user);
		RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
		ReportCreateRequest request = new ReportCreateRequest();
		request.setTargetType(ReportTargetType.FOUND_ITEM);
		request.setTargetId(7L);
		request.setReasonType(ReportReasonType.FRAUD);
		request.setReturnUrl("https://example.com/phishing");

		when(postReportService.fallbackTargetUrl(ReportTargetType.FOUND_ITEM, 7L)).thenReturn("/found-items/7");
		when(postReportService.create(request, user)).thenReturn("/found-items/7");

		String view = controller.create(request, userDetails, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/found-items/7");
	}
}
