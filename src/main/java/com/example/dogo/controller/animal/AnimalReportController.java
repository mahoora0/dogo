package com.example.dogo.controller.animal;

import com.example.dogo.dto.animal.AnimalReportCreateRequest;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.animal.AnimalReportService;
import com.example.dogo.service.item.RegistrationOptionService;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AnimalReportController {

	private static final int MAX_PAGE_SIZE = 30;
	private static final java.util.Set<String> SORT_FIELDS = java.util.Set.of("regdate", "eventDate");

	private final AnimalReportService animalReportService;
	private final RegistrationOptionService registrationOptionService;

	@ModelAttribute("reportTypeOptions")
	public List<Option> reportTypeOptions() {
		return List.of(new Option("MISSING", "실종"), new Option("SIGHTING", "목격"));
	}

	@ModelAttribute("animalTypeOptions")
	public List<Option> animalTypeOptions() {
		return List.of(new Option("DOG", "개"), new Option("CAT", "고양이"), new Option("OTHER", "기타"));
	}

	@ModelAttribute("genderOptions")
	public List<Option> genderOptions() {
		return List.of(new Option("UNKNOWN", "모름"), new Option("MALE", "수컷"), new Option("FEMALE", "암컷"));
	}

	@ModelAttribute("neuteredStatusOptions")
	public List<Option> neuteredStatusOptions() {
		return List.of(
				new Option("UNKNOWN", "모름"),
				new Option("NEUTERED", "중성화 완료"),
				new Option("NOT_NEUTERED", "중성화 안 됨")
		);
	}

	@ModelAttribute("ageUnitOptions")
	public List<Option> ageUnitOptions() {
		return List.of(new Option("YEAR", "살"), new Option("MONTH", "개월"), new Option("UNKNOWN", "모름"));
	}

	@ModelAttribute("careStatusOptions")
	public List<Option> careStatusOptions() {
		return List.of(
				new Option("UNKNOWN", "확인 필요"),
				new Option("NONE", "단순 목격"),
				new Option("PROTECTING", "보호 중"),
				new Option("TRANSFERRED", "보호소/병원 인계")
		);
	}

	@ModelAttribute("regionOptions")
	public List<String> regionOptions() {
		return registrationOptionService.getRegionOptions();
	}

	@GetMapping("/animal-reports")
	public String list(
			@RequestParam(required = false) String reportType,
			@RequestParam(required = false) String animalType,
			@RequestParam(required = false) String region,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "regdate") String sortBy,
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
		PageRequest pageRequest = PageRequest.of(safePage, safeSize, sort);
		Page<?> reportPage = animalReportService.search(reportType, animalType, region, keyword, pageRequest);
		if (safePage > 0 && safePage >= reportPage.getTotalPages() && reportPage.getTotalPages() > 0) {
			safePage = reportPage.getTotalPages() - 1;
			reportPage = animalReportService.search(reportType, animalType, region, keyword, PageRequest.of(safePage, safeSize, sort));
		}

		model.addAttribute("reportPage", reportPage);
		model.addAttribute("reports", reportPage.getContent());
		model.addAttribute("reportType", reportType);
		model.addAttribute("animalType", animalType);
		model.addAttribute("region", region);
		model.addAttribute("keyword", keyword);
		model.addAttribute("sortBy", safeField);
		model.addAttribute("sortDir", sortDir);
		model.addAttribute("page", safePage);
		model.addAttribute("size", safeSize);
		model.addAttribute("currentUri", "/animal-reports");
		return "animal-reports/list";
	}

	@GetMapping("/animal-reports/new")
	public String createForm(Model model) {
		model.addAttribute("request", new AnimalReportCreateRequest());
		model.addAttribute("currentUri", "/animal-reports/new");
		return "animal-reports/new";
	}

	@PostMapping("/animal-reports")
	public String create(
			@ModelAttribute("request") AnimalReportCreateRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Model model
	) {
		try {
			Long reportId = animalReportService.create(request, userDetails != null ? userDetails.getUser() : null);
			return "redirect:/animal-reports/" + reportId;
		} catch (IllegalArgumentException exception) {
			model.addAttribute("errorMessage", exception.getMessage());
			model.addAttribute("currentUri", "/animal-reports/new");
			return "animal-reports/new";
		} catch (Exception exception) {
			model.addAttribute("errorMessage", "등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
			model.addAttribute("currentUri", "/animal-reports/new");
			return "animal-reports/new";
		}
	}

	@GetMapping("/animal-reports/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("report", animalReportService.getDetail(id));
		model.addAttribute("currentUri", "/animal-reports");
		return "animal-reports/detail";
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String notFound(IllegalArgumentException exception, Model model) {
		model.addAttribute("message", exception.getMessage());
		return "animal-reports/error";
	}

	public record Option(String value, String label) {
	}
}
