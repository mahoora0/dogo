package com.example.dogo.service.animal;

import com.example.dogo.dto.animal.AnimalReportCreateRequest;
import com.example.dogo.dto.animal.AnimalReportDetailView;
import com.example.dogo.dto.animal.AnimalReportView;
import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.animal.AnimalReportImage;
import com.example.dogo.entity.area.Area;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.animal.AnimalReportImageRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.area.AreaRepository;
import com.example.dogo.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AnimalReportService {

	private static final String DEV_USER_EMAIL = "dev@dogo.local";
	private static final String PLACEHOLDER_IMAGE = "/images/noImageSize.png";
	private static final Set<String> REPORT_TYPES = Set.of("MISSING", "SIGHTING");
	private static final Set<String> ANIMAL_TYPES = Set.of("DOG", "CAT", "OTHER");
	private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "UNKNOWN");
	private static final Set<String> NEUTERED_STATUSES = Set.of("NEUTERED", "NOT_NEUTERED", "UNKNOWN");
	private static final Set<String> AGE_UNITS = Set.of("MONTH", "YEAR", "UNKNOWN");
	private static final Set<String> CARE_STATUSES = Set.of("NONE", "PROTECTING", "TRANSFERRED", "UNKNOWN");

	private final AnimalReportRepository animalReportRepository;
	private final AnimalReportImageRepository animalReportImageRepository;
	private final AreaRepository areaRepository;
	private final UserRepository userRepository;
	private final Path animalReportUploadPath;

	public AnimalReportService(
			AnimalReportRepository animalReportRepository,
			AnimalReportImageRepository animalReportImageRepository,
			AreaRepository areaRepository,
			UserRepository userRepository,
			@Value("${file.upload-dir}") String uploadDir
	) {
		this.animalReportRepository = animalReportRepository;
		this.animalReportImageRepository = animalReportImageRepository;
		this.areaRepository = areaRepository;
		this.userRepository = userRepository;
		this.animalReportUploadPath = Path.of(uploadDir, "animal-reports").toAbsolutePath().normalize();
	}

	@Transactional(readOnly = true)
	public Page<AnimalReportView> search(String reportType, String animalType, String region, String keyword, Pageable pageable) {
		return animalReportRepository.search(
				blankToNull(reportType),
				blankToNull(animalType),
				blankToNull(region),
				blankToNull(keyword),
				pageable
		).map(this::toListView);
	}

	@Transactional
	public Long create(AnimalReportCreateRequest request, User loginUser) {
		validateCreateRequest(request);

		User user = (loginUser != null) ? loginUser : getOrCreateDevUser();
		Area regionArea = findRegionArea(request.getRegionName()).orElse(null);
		String reportType = request.getReportType().trim();
		String animalType = request.getAnimalType().trim();
		String breedName = blankToNull(request.getBreedName());
		String title = defaultTitle(reportType, request.getTitle(), animalType, breedName);

		AnimalReport report = new AnimalReport(
				user,
				reportType,
				title,
				request.getEventDate(),
				request.getEventTime(),
				regionArea,
				request.getRegionName().trim(),
				request.getDetailPlace().trim(),
				blankToNull(request.getContactPhone()),
				request.isContactPublic(),
				normalizedCareStatus(reportType, request.getSightingCareStatus()),
				animalType,
				breedName,
				defaultInSet(request.getGender(), GENDERS, "UNKNOWN"),
				defaultInSet(request.getNeuteredStatus(), NEUTERED_STATUSES, "UNKNOWN"),
				request.getAgeValue(),
				normalizedAgeUnit(request.getAgeUnit()),
				normalizedWeight(request.getWeightKg()),
				blankToNull(request.getFurColor()),
				blankToNull(request.getDistinctiveMarks()),
				blankToNull(request.getContent())
		);

		AnimalReport savedReport = animalReportRepository.save(report);
		saveImages(savedReport, request.getUploadImages());
		return savedReport.getReportId();
	}

	@Transactional
	public AnimalReportDetailView getDetail(Long id) {
		AnimalReport report = animalReportRepository.findById(id)
				.filter(candidate -> !candidate.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("동물 신고 게시글을 찾을 수 없습니다."));
		report.increaseViewCount();

		List<String> imageUrls = animalReportImageRepository.findByAnimalReportOrderBySortOrderAscImageIdAsc(report).stream()
				.map(AnimalReportImage::getImageUrl)
				.toList();
		if (imageUrls.isEmpty()) {
			imageUrls = List.of(PLACEHOLDER_IMAGE);
		}

		return new AnimalReportDetailView(
				report.getReportId(),
				report.getReportType(),
				reportTypeLabel(report.getReportType()),
				report.getStatus(),
				statusLabel(report.getStatus()),
				report.getTitle(),
				report.getEventDate(),
				report.getEventTime(),
				report.getRegionName(),
				report.getDetailPlace(),
				report.getContactPhone(),
				report.isContactPublic(),
				displayContact(report),
				report.getSightingCareStatus(),
				careStatusLabel(report.getSightingCareStatus()),
				report.getAnimalType(),
				animalTypeLabel(report.getAnimalType()),
				report.getBreedName(),
				report.getGender(),
				genderLabel(report.getGender()),
				report.getNeuteredStatus(),
				neuteredStatusLabel(report.getNeuteredStatus()),
				report.getAgeValue(),
				report.getAgeUnit(),
				ageUnitLabel(report.getAgeUnit()),
				report.getWeightKg(),
				ageDisplay(report.getAgeValue(), report.getAgeUnit()),
				weightDisplay(report.getWeightKg()),
				report.getFurColor(),
				report.getDistinctiveMarks(),
				report.getContent(),
				report.getViewCount(),
				imageUrls
		);
	}

	private AnimalReportView toListView(AnimalReport report) {
		String imageUrl = animalReportImageRepository.findFirstByAnimalReportOrderBySortOrderAscImageIdAsc(report)
				.map(AnimalReportImage::getImageUrl)
				.orElse(PLACEHOLDER_IMAGE);

		return new AnimalReportView(
				report.getReportId(),
				report.getReportType(),
				reportTypeLabel(report.getReportType()),
				report.getStatus(),
				statusLabel(report.getStatus()),
				report.getTitle(),
				report.getEventDate(),
				report.getEventTime(),
				report.getRegionName(),
				report.getDetailPlace(),
				report.getAnimalType(),
				animalTypeLabel(report.getAnimalType()),
				report.getBreedName(),
				report.getFurColor(),
				report.getSightingCareStatus(),
				careStatusLabel(report.getSightingCareStatus()),
				imageUrl
		);
	}

	private void validateCreateRequest(AnimalReportCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("동물 신고 정보를 입력해주세요.");
		}
		if (!REPORT_TYPES.contains(defaultText(request.getReportType(), "").trim())) {
			throw new IllegalArgumentException("신고 구분을 선택해주세요.");
		}
		if (request.getEventDate() == null) {
			throw new IllegalArgumentException("날짜를 입력해주세요.");
		}
		if (request.getEventDate().isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("미래 날짜는 입력할 수 없습니다.");
		}
		if (!StringUtils.hasText(request.getRegionName())) {
			throw new IllegalArgumentException("지역을 선택해주세요.");
		}
		if (!StringUtils.hasText(request.getDetailPlace())) {
			throw new IllegalArgumentException("구체적인 장소를 입력해주세요.");
		}
		if (!ANIMAL_TYPES.contains(defaultText(request.getAnimalType(), "").trim())) {
			throw new IllegalArgumentException("동물 종류를 선택해주세요.");
		}
		if (request.getAgeValue() != null && request.getAgeValue() < 0) {
			throw new IllegalArgumentException("나이는 0 이상으로 입력해주세요.");
		}
		if (request.getWeightKg() != null && request.getWeightKg().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("몸무게는 0 이상으로 입력해주세요.");
		}
	}

	private void saveImages(AnimalReport report, List<MultipartFile> images) {
		for (int index = 0; index < images.size(); index++) {
			saveImageIfPresent(report, images.get(index), index);
		}
	}

	private void saveImageIfPresent(AnimalReport report, MultipartFile image, int sortOrder) {
		if (image == null || image.isEmpty()) {
			return;
		}

		try {
			Files.createDirectories(animalReportUploadPath);

			String originalName = StringUtils.cleanPath(String.valueOf(image.getOriginalFilename()));
			String extension = extractExtension(originalName);
			String storedName = UUID.randomUUID() + extension;
			Path targetPath = animalReportUploadPath.resolve(storedName).normalize();
			if (!targetPath.startsWith(animalReportUploadPath)) {
				throw new IllegalArgumentException("올바르지 않은 이미지 파일명입니다.");
			}

			image.transferTo(targetPath);

			animalReportImageRepository.save(new AnimalReportImage(
					report,
					originalName,
					storedName,
					"/uploads/animal-reports/" + storedName,
					image.getContentType(),
					image.getSize(),
					sortOrder
			));
		} catch (IOException exception) {
			throw new UncheckedIOException("이미지 저장에 실패했습니다.", exception);
		}
	}

	private Optional<Area> findRegionArea(String regionName) {
		if (!StringUtils.hasText(regionName)) {
			return Optional.empty();
		}
		return areaRepository.findByAreaName(regionName.trim()).stream()
				.filter(area -> area.getParentAreaId() == null)
				.findFirst();
	}

	private User getOrCreateDevUser() {
		return userRepository.findByEmail(DEV_USER_EMAIL)
				.orElseGet(() -> userRepository.save(new User(DEV_USER_EMAIL, "개발용 사용자", "010-0000-0000")));
	}

	private String defaultTitle(String reportType, String title, String animalType, String breedName) {
		if (StringUtils.hasText(title)) {
			return title.trim();
		}
		String subject = StringUtils.hasText(breedName) ? breedName.trim() : animalTypeLabel(animalType);
		if ("MISSING".equals(reportType)) {
			return subject + "을 찾습니다";
		}
		return subject + "을 목격했어요";
	}

	private String normalizedCareStatus(String reportType, String careStatus) {
		if (!"SIGHTING".equals(reportType)) {
			return null;
		}
		return defaultInSet(careStatus, CARE_STATUSES, "UNKNOWN");
	}

	private String normalizedAgeUnit(String ageUnit) {
		if (!StringUtils.hasText(ageUnit)) {
			return null;
		}
		return AGE_UNITS.contains(ageUnit.trim()) ? ageUnit.trim() : "UNKNOWN";
	}

	private BigDecimal normalizedWeight(BigDecimal weightKg) {
		if (weightKg == null) {
			return null;
		}
		return weightKg;
	}

	private String displayContact(AnimalReport report) {
		if (!report.isContactPublic()) {
			return "비공개";
		}
		if (!StringUtils.hasText(report.getContactPhone())) {
			return "-";
		}
		return report.getContactPhone();
	}

	private String extractExtension(String filename) {
		if (!StringUtils.hasText(filename) || !filename.contains(".")) {
			return "";
		}
		String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
		if (extension.length() > 12) {
			return "";
		}
		return extension;
	}

	private String defaultInSet(String value, Set<String> allowed, String fallback) {
		if (!StringUtils.hasText(value)) {
			return fallback;
		}
		String normalized = value.trim();
		return allowed.contains(normalized) ? normalized : fallback;
	}

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private String defaultText(String value, String fallback) {
		if (StringUtils.hasText(value)) {
			return value;
		}
		return fallback;
	}

	private String reportTypeLabel(String reportType) {
		return switch (defaultText(reportType, "")) {
			case "MISSING" -> "실종";
			case "SIGHTING" -> "목격";
			default -> "신고";
		};
	}

	private String statusLabel(String status) {
		return switch (defaultText(status, "")) {
			case "MATCHING" -> "매칭중";
			case "RESOLVED" -> "해결";
			case "CLOSED" -> "종료";
			default -> "진행중";
		};
	}

	private String animalTypeLabel(String animalType) {
		return switch (defaultText(animalType, "")) {
			case "DOG" -> "개";
			case "CAT" -> "고양이";
			case "OTHER" -> "기타";
			default -> "동물";
		};
	}

	private String genderLabel(String gender) {
		return switch (defaultText(gender, "")) {
			case "MALE" -> "수컷";
			case "FEMALE" -> "암컷";
			default -> "모름";
		};
	}

	private String neuteredStatusLabel(String neuteredStatus) {
		return switch (defaultText(neuteredStatus, "")) {
			case "NEUTERED" -> "중성화 완료";
			case "NOT_NEUTERED" -> "중성화 안 됨";
			default -> "모름";
		};
	}

	private String ageUnitLabel(String ageUnit) {
		return switch (defaultText(ageUnit, "")) {
			case "MONTH" -> "개월";
			case "YEAR" -> "살";
			default -> "";
		};
	}

	private String ageDisplay(Integer ageValue, String ageUnit) {
		if (ageValue == null) {
			return "-";
		}
		if (ageValue == 0 && "YEAR".equals(ageUnit)) {
			return "1년 미만";
		}
		return ageValue + ageUnitLabel(ageUnit);
	}

	private String weightDisplay(BigDecimal weightKg) {
		if (weightKg == null) {
			return null;
		}
		if (weightKg.compareTo(new BigDecimal("0.5")) == 0) {
			return "1kg 미만";
		}
		return weightKg.stripTrailingZeros().toPlainString() + "kg";
	}

	private String careStatusLabel(String careStatus) {
		return switch (defaultText(careStatus, "")) {
			case "NONE" -> "단순 목격";
			case "PROTECTING" -> "보호 중";
			case "TRANSFERRED" -> "보호소/병원 인계";
			case "UNKNOWN" -> "확인 필요";
			default -> null;
		};
	}
}
