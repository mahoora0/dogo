package com.example.dogo.service.animal;

import com.example.dogo.dto.animal.AnimalImageSearchResult;
import com.example.dogo.dto.animal.AnimalMatchCandidateView;
import com.example.dogo.dto.animal.AnimalReportCreateRequest;
import com.example.dogo.dto.item.RecentItemView;
import com.example.dogo.dto.animal.AnimalReportDetailView;
import com.example.dogo.dto.animal.AnimalReportEditData;
import com.example.dogo.dto.animal.AnimalReportView;
import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.animal.AnimalReportImage;
import com.example.dogo.entity.animal.AnimalReportMatch;
import com.example.dogo.entity.area.Area;
import com.example.dogo.entity.user.User;
import com.example.dogo.service.upload.UploadFileValidator;
import com.example.dogo.repository.animal.AnimalReportImageRepository;
import com.example.dogo.repository.animal.AnimalReportMatchRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.area.AreaRepository;
import com.example.dogo.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnimalReportService {

	private static final String DEV_USER_EMAIL = "dev@dogo.local";
	private static final String PLACEHOLDER_IMAGE = "/images/noImageSize.png";
	private static final Set<String> USER_REPORT_TYPES = Set.of("MISSING", "SIGHTING");
	private static final Set<String> ANIMAL_TYPES = Set.of("DOG", "CAT", "OTHER");
	private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "UNKNOWN");
	private static final Set<String> NEUTERED_STATUSES = Set.of("NEUTERED", "NOT_NEUTERED", "UNKNOWN");
	private static final Set<String> AGE_UNITS = Set.of("MONTH", "YEAR", "UNKNOWN", "RANGE_0_1", "RANGE_2_5", "RANGE_6_10", "RANGE_10_OVER", "ESTIMATED");
	private static final Set<String> CARE_STATUSES = Set.of("NONE", "PROTECTING", "TRANSFERRED", "UNKNOWN");

	private final AnimalReportRepository animalReportRepository;
	private final AnimalReportImageRepository animalReportImageRepository;
	private final AnimalReportMatchRepository animalReportMatchRepository;
	private final AreaRepository areaRepository;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final Path animalReportUploadPath;
	private final Optional<AnimalImageEmbeddingService> imageEmbeddingService;

	public AnimalReportService(
			AnimalReportRepository animalReportRepository,
			AnimalReportImageRepository animalReportImageRepository,
			AnimalReportMatchRepository animalReportMatchRepository,
			AreaRepository areaRepository,
			UserRepository userRepository,
			ApplicationEventPublisher eventPublisher,
			Optional<AnimalImageEmbeddingService> imageEmbeddingService,
			@Value("${file.upload-dir}") String uploadDir
	) {
		this.animalReportRepository = animalReportRepository;
		this.animalReportImageRepository = animalReportImageRepository;
		this.animalReportMatchRepository = animalReportMatchRepository;
		this.areaRepository = areaRepository;
		this.userRepository = userRepository;
		this.eventPublisher = eventPublisher;
		this.imageEmbeddingService = imageEmbeddingService;
		this.animalReportUploadPath = Path.of(uploadDir, "animal-reports").toAbsolutePath().normalize();
	}

	@Transactional(readOnly = true)
	public Page<AnimalReportView> search(
			String reportType,
			String animalType,
			String region,
			String keyword,
			String keywordScope,
			Pageable pageable
	) {
		return search(reportType, animalType, region, keyword, keywordScope, null, null, null, pageable);
	}

	@Transactional(readOnly = true)
	public Page<AnimalReportView> search(
			String reportType,
			String animalType,
			String region,
			String keyword,
			String keywordScope,
			java.time.LocalDate startDate,
			java.time.LocalDate endDate,
			String detailPlace,
			Pageable pageable
	) {
		Page<AnimalReport> page = animalReportRepository.findAll(
				animalReportSearchSpec(reportType, animalType, region, keyword, keywordScope, startDate, endDate, detailPlace),
				pageable
		);
		Map<Long, String> thumbnails = resolveThumbnails(page.getContent());
		return page.map(report -> toListView(report, thumbnails));
	}

	@Transactional(readOnly = true)
	public List<RecentItemView> getRecentItems(int limit) {
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "eventDate").and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "reportId")));
		List<AnimalReport> reports = animalReportRepository.findAll(animalReportSearchSpec(null, null, null, null, null, null, null, null), pageable).getContent();
		Map<Long, String> thumbnails = resolveThumbnails(reports);
		return reports.stream()
				.map(item -> new RecentItemView(
						item.getReportId(),
						"ANIMAL",
						reportTypeLabel(item.getReportType()),
						item.getTitle(),
						animalTypeLabel(item.getAnimalType()),
						item.getRegionName(),
						item.getEventDate() != null ? item.getEventDate().atStartOfDay() : null,
						item.getStatus(),
						statusLabel(item.getStatus()),
						thumbnails.getOrDefault(item.getReportId(), PLACEHOLDER_IMAGE),
						item.getRegdate()
				))
				.toList();
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
		report.updateCareLocation(
				careLocationName(request),
				careLocationAddress(request),
				careContactPhone(request)
		);

		AnimalReport savedReport = animalReportRepository.save(report);
		saveImages(savedReport, request.getUploadImages());
		eventPublisher.publishEvent(new AnimalReportCreatedEvent(savedReport.getReportId()));
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
		PublicAnimalDetails publicDetails = publicAnimalDetails(report);

		String careStatus = getDetailCareStatus(report.getReportType(), report.getSightingCareStatus());
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
				locationSummary(report, publicDetails),
				report.getContactPhone(),
				report.isContactPublic(),
				displayContact(report),
				careStatus,
				careStatusLabel(careStatus),
				report.getCareLocationName(),
				report.getCareLocationAddress(),
				report.getCareContactPhone(),
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
				displayAge(report, publicDetails),
				weightDisplay(report.getWeightKg()),
				report.getFurColor(),
				report.getDistinctiveMarks(),
				report.getContent(),
				displayContent(report.getContent(), publicDetails.hasAny()),
				publicDetails.shelterName(),
				publicDetails.shelterAddress(),
				publicDetails.authorityName(),
				publicDetails.noticeNo(),
				publicDetails.noticePeriod(),
				report.getViewCount(),
				imageUrls,
				report.getUser() != null ? report.getUser().getUserNo() : null
		);
	}

	@Transactional(readOnly = true)
	public AnimalReportEditData getForEdit(Long id, User loginUser) {
		AnimalReport report = animalReportRepository.findById(id)
				.filter(r -> !r.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("동물 신고 게시글을 찾을 수 없습니다."));
		checkOwnership(report, loginUser);

		List<String> imageUrls = animalReportImageRepository.findByAnimalReportOrderBySortOrderAscImageIdAsc(report).stream()
				.map(AnimalReportImage::getImageUrl)
				.toList();

		return new AnimalReportEditData(
				report.getReportId(),
				getFormReportType(report.getReportType()),
				report.getTitle(),
				report.getEventDate(),
				report.getEventTime(),
				report.getRegionName(),
				report.getDetailPlace(),
				report.getContactPhone(),
				report.isContactPublic(),
				getFormCareStatus(report.getReportType(), report.getSightingCareStatus()),
				report.getCareLocationName(),
				report.getCareLocationAddress(),
				report.getCareContactPhone(),
				report.getAnimalType(),
				report.getBreedName(),
				report.getGender(),
				report.getNeuteredStatus(),
				report.getAgeValue(),
				report.getAgeUnit(),
				report.getWeightKg(),
				report.getFurColor(),
				report.getDistinctiveMarks(),
				report.getContent(),
				imageUrls
		);
	}

	@Transactional
	public void update(Long id, AnimalReportCreateRequest request, User loginUser) {
		validateCreateRequest(request);
		AnimalReport report = animalReportRepository.findById(id)
				.filter(r -> !r.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("동물 신고 게시글을 찾을 수 없습니다."));
		checkOwnership(report, loginUser);

		String reportType = request.getReportType().trim();
		String animalType = request.getAnimalType().trim();
		String breedName = blankToNull(request.getBreedName());
		String title = defaultTitle(reportType, request.getTitle(), animalType, breedName);
		Area regionArea = findRegionArea(request.getRegionName()).orElse(null);

		report.update(
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
		report.updateCareLocation(
				careLocationName(request),
				careLocationAddress(request),
				careContactPhone(request)
		);

		List<MultipartFile> newImages = request.getUploadImages();
		if (!newImages.isEmpty()) {
			List<AnimalReportImage> oldImages = animalReportImageRepository.findByAnimalReportOrderBySortOrderAscImageIdAsc(report);
			for (AnimalReportImage old : oldImages) {
				try {
					Files.deleteIfExists(animalReportUploadPath.resolve(old.getStoredName()));
				} catch (IOException ignored) {
				}
			}
			animalReportImageRepository.deleteAll(oldImages);
			saveImages(report, newImages);
		}

		clearMatchesForReport(report.getReportId());
		eventPublisher.publishEvent(new AnimalReportCreatedEvent(report.getReportId()));
	}

	@Transactional
	public void clearMatchesForReport(Long reportId) {
		animalReportMatchRepository.deleteByMissingReport_ReportId(reportId);
		animalReportMatchRepository.deleteBySightingReport_ReportId(reportId);
		animalReportMatchRepository.flush();
	}

	@Transactional
	public void updateStatus(Long id, String status, User loginUser) {
		AnimalReport report = animalReportRepository.findById(id)
				.filter(r -> !r.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("동물 신고 게시글을 찾을 수 없습니다."));
		checkOwnership(report, loginUser);
		report.setStatus(normalizeStatus(status));
	}

	@Transactional
	public void delete(Long id, User loginUser) {
		AnimalReport report = animalReportRepository.findById(id)
				.filter(r -> !r.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("동물 신고 게시글을 찾을 수 없습니다."));
		checkOwnership(report, loginUser);

		clearMatchesForReport(report.getReportId());
		report.markDeleted();
	}

	private void checkOwnership(AnimalReport report, User loginUser) {
		if (loginUser == null || report.getUser() == null || !report.getUser().getUserNo().equals(loginUser.getUserNo())) {
			throw new IllegalArgumentException("수정 권한이 없습니다.");
		}
	}

	private Map<Long, String> resolveThumbnails(List<AnimalReport> reports) {
		if (reports.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> thumbnails = new HashMap<>();
		for (AnimalReportImage image : animalReportImageRepository.findByAnimalReportInOrderBySortOrderAscImageIdAsc(reports)) {
			thumbnails.putIfAbsent(image.getAnimalReport().getReportId(), image.getImageUrl());
		}
		return thumbnails;
	}

	private AnimalReportView toListView(AnimalReport report, Map<Long, String> thumbnails) {
		String imageUrl = thumbnails.getOrDefault(report.getReportId(), PLACEHOLDER_IMAGE);
		String careStatus = getDetailCareStatus(report.getReportType(), report.getSightingCareStatus());

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
				careStatus,
				careStatusLabel(careStatus),
				imageUrl
		);
	}

	private void validateCreateRequest(AnimalReportCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("동물 신고 정보를 입력해주세요.");
		}
		if (!USER_REPORT_TYPES.contains(defaultText(request.getReportType(), "").trim())) {
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
		String reportType = request.getReportType().trim();
		String careStatus = normalizedCareStatus(reportType, request.getSightingCareStatus());
		if (requiresCareLocation(careStatus) && !StringUtils.hasText(request.getCareLocationAddress())) {
			throw new IllegalArgumentException("동물을 인계한 장소의 주소를 입력해주세요.");
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
			String extension = UploadFileValidator.imageExtension(image);
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

	private Specification<AnimalReport> animalReportSearchSpec(
			String reportType,
			String animalType,
			String region,
			String keyword,
			String keywordScope,
			java.time.LocalDate startDate,
			java.time.LocalDate endDate,
			String detailPlace
	) {
		return (root, query, criteriaBuilder) -> {
			// 사진이 여러 장인 게시물이 중복 표시되는 것을 방지 (OneToMany images 조인으로 인한 카르테시안 곱 제거)
			if (query != null) {
				query.distinct(true);
			}
			List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
			predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

			String normalizedReportType = blankToNull(reportType);
			if (normalizedReportType != null) {
				if ("SIGHTING".equals(normalizedReportType)) {
					predicates.add(root.get("reportType").in("SIGHTING", "PROTECTING"));
				} else {
					predicates.add(criteriaBuilder.equal(root.get("reportType"), normalizedReportType));
				}
			}

			String normalizedAnimalType = blankToNull(animalType);
			if (normalizedAnimalType != null) {
				predicates.add(criteriaBuilder.equal(root.get("animalType"), normalizedAnimalType));
			}

			String normalizedRegion = blankToNull(region);
			if (normalizedRegion != null) {
				predicates.add(criteriaBuilder.like(
						criteriaBuilder.lower(root.get("regionName")),
						"%" + normalizedRegion.toLowerCase(Locale.ROOT) + "%"
				));
			}

			if (startDate != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), startDate));
			}
			if (endDate != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), endDate));
			}

			String normalizedDetailPlace = blankToNull(detailPlace);
			if (normalizedDetailPlace != null) {
				predicates.add(criteriaBuilder.like(
						criteriaBuilder.lower(root.get("detailPlace")),
						"%" + normalizedDetailPlace.toLowerCase(Locale.ROOT) + "%"
				));
			}

			String normalizedKeyword = blankToNull(keyword);
			if (normalizedKeyword != null) {
				String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
				List<jakarta.persistence.criteria.Predicate> keywordPredicates = searchFields(keywordScope).stream()
						.map(field -> criteriaBuilder.like(
								criteriaBuilder.lower(root.get(field)),
								pattern
						))
						.toList();
				predicates.add(criteriaBuilder.or(keywordPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
			}

			return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	private List<String> searchFields(String keywordScope) {
		return switch (defaultText(keywordScope, "ALL").trim()) {
			case "TITLE_PLACE" -> List.of("title", "detailPlace", "regionName");
			case "BREED_FEATURE" -> List.of("breedName", "distinctiveMarks");
			case "CONTENT" -> List.of("content");
			case "COLOR" -> List.of("furColor");
			default -> List.of("title", "breedName", "furColor", "distinctiveMarks", "content", "detailPlace", "regionName");
		};
	}

	private User getOrCreateDevUser() {
		return userRepository.findByEmail(DEV_USER_EMAIL)
				.orElseGet(() -> userRepository.save(new User(DEV_USER_EMAIL, "개발용 사용자")));
	}

	private String defaultTitle(String reportType, String title, String animalType, String breedName) {
		if (StringUtils.hasText(title)) {
			return title.trim();
		}
		String subject = StringUtils.hasText(breedName) ? breedName.trim() : animalTypeLabel(animalType);
		return switch (reportType) {
			case "MISSING" -> subject + "을 찾습니다";
			case "SIGHTING" -> subject + "을 목격했어요";
			case "PROTECTING" -> subject + "을 보호하고 있어요";
			case "RETURNED" -> subject + "가 귀가했어요";
			case "TRANSFERRED" -> subject + "를 인계했어요";
			default -> subject + " 관련 신고";
		};
	}

	private String normalizedCareStatus(String reportType, String careStatus) {
		if (!"SIGHTING".equals(defaultText(reportType, "").trim())) {
			return null;
		}
		return defaultInSet(careStatus, CARE_STATUSES, "UNKNOWN");
	}

	private boolean requiresCareLocation(String careStatus) {
		return "TRANSFERRED".equals(careStatus);
	}

	private String careLocationName(AnimalReportCreateRequest request) {
		return requiresCareLocation(normalizedCareStatus(request.getReportType(), request.getSightingCareStatus()))
				? blankToNull(request.getCareLocationName())
				: null;
	}

	private String careLocationAddress(AnimalReportCreateRequest request) {
		return requiresCareLocation(normalizedCareStatus(request.getReportType(), request.getSightingCareStatus()))
				? blankToNull(request.getCareLocationAddress())
				: null;
	}

	private String careContactPhone(AnimalReportCreateRequest request) {
		return requiresCareLocation(normalizedCareStatus(request.getReportType(), request.getSightingCareStatus()))
				? blankToNull(request.getCareContactPhone())
				: null;
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

	private String locationSummary(AnimalReport report, PublicAnimalDetails publicDetails) {
		if (publicDetails.hasAny()) {
			return joinPresent(publicDetails.authorityName(), report.getDetailPlace());
		}
		return joinPresent(report.getRegionName(), report.getDetailPlace());
	}

	private String displayAge(AnimalReport report, PublicAnimalDetails publicDetails) {
		String publicAgeText = blankToNull(publicDetails.ageText());
		if (publicAgeText != null) {
			return publicAgeText;
		}
		return ageDisplay(report.getAgeValue(), report.getAgeUnit());
	}

	private String displayContent(String content, boolean publicApiReport) {
		String value = blankToNull(content);
		if (value == null) {
			return null;
		}
		List<String> visibleLines = value.lines()
				.filter(line -> !isHiddenContentLine(line, publicApiReport))
				.toList();
		String result = String.join("\n", visibleLines).trim();
		return StringUtils.hasText(result) ? result : null;
	}

	private boolean isHiddenContentLine(String line, boolean publicApiReport) {
		String trimmed = line == null ? "" : line.trim();
		if (trimmed.startsWith("보호소:")
				|| trimmed.startsWith("보호소 주소:")
				|| trimmed.startsWith("관할기관:")
				|| trimmed.startsWith("공고번호:")
				|| trimmed.startsWith("공고기간:")) {
			return true;
		}
		return publicApiReport && (
				trimmed.startsWith("상태:")
						|| trimmed.startsWith("특징:")
						|| trimmed.startsWith("나이:")
						|| trimmed.startsWith("체중:")
		);
	}

	private PublicAnimalDetails publicAnimalDetails(AnimalReport report) {
		if (report == null || !StringUtils.hasText(report.getRawPayload())) {
			return PublicAnimalDetails.empty();
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(report.getRawPayload())));
			document.getDocumentElement().normalize();
			return new PublicAnimalDetails(
					text(document, "careNm"),
					text(document, "careAddr"),
					text(document, "orgNm"),
					text(document, "noticeNo"),
					noticePeriod(text(document, "noticeSdt"), text(document, "noticeEdt")),
					text(document, "age")
			);
		} catch (Exception ignored) {
			return PublicAnimalDetails.empty();
		}
	}

	private String text(Document document, String tagName) {
		var nodes = document.getElementsByTagName(tagName);
		if (nodes.getLength() == 0) {
			return null;
		}
		return blankToNull(nodes.item(0).getTextContent());
	}

	private String noticePeriod(String startDate, String endDate) {
		String start = displayDate(startDate);
		String end = displayDate(endDate);
		if (start == null && end == null) {
			return null;
		}
		if (start == null) {
			return end;
		}
		if (end == null || start.equals(end)) {
			return start;
		}
		return start + " ~ " + end;
	}

	private String displayDate(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.replace("-", "");
		if (normalized.length() != 8) {
			return value.trim();
		}
		return normalized.substring(0, 4) + "-" + normalized.substring(4, 6) + "-" + normalized.substring(6, 8);
	}

	private String joinPresent(String first, String second) {
		String normalizedFirst = blankToNull(first);
		String normalizedSecond = blankToNull(second);
		if (normalizedFirst == null) {
			return normalizedSecond;
		}
		if (normalizedSecond == null) {
			return normalizedFirst;
		}
		return normalizedFirst + " · " + normalizedSecond;
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

	private String normalizeStatus(String status) {
		String normalizedStatus = blankToNull(status);
		if ("OPEN".equals(normalizedStatus) || "RESOLVED".equals(normalizedStatus) || "CLOSED".equals(normalizedStatus)) {
			return normalizedStatus;
		}
		throw new IllegalArgumentException("변경할 수 없는 상태입니다.");
	}

	private String reportTypeLabel(String reportType) {
		return switch (defaultText(reportType, "")) {
			case "MISSING" -> "실종";
			case "SIGHTING" -> "목격";
			case "PROTECTING" -> "보호";
			case "RETURNED" -> "귀가";
			case "TRANSFERRED" -> "연계";
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
			case "YEAR" -> "세";
			case "RANGE_0_1" -> "0~1세";
			case "RANGE_2_5" -> "2~5세";
			case "RANGE_6_10" -> "6~10세";
			case "RANGE_10_OVER" -> "10세 이상";
			case "ESTIMATED" -> "미상";
			default -> "";
		};
	}

	private String ageDisplay(Integer ageValue, String ageUnit) {
		String unitLabel = ageUnitLabel(ageUnit);
		// 구간 방식: ageValue 없이 ageUnit만으로 표시
		if (ageUnit != null && (ageUnit.startsWith("RANGE_") || "ESTIMATED".equals(ageUnit))) {
			return unitLabel;
		}
		if (ageValue == null) {
			return null;
		}
		if (ageValue == 0 && "YEAR".equals(ageUnit)) {
			return "1년 미만";
		}
		return ageValue + unitLabel;
	}

	private String weightDisplay(BigDecimal weightKg) {
		if (weightKg == null) {
			return null;
		}
		double val = weightKg.doubleValue();
		if (val >= 0 && val <= 1.0) {
			return "0~1kg";
		} else if (val > 1.0 && val <= 3.0) {
			return "1~3kg";
		} else if (val > 3.0 && val <= 5.0) {
			return "3~5kg";
		} else if (val > 5.0 && val <= 10.0) {
			return "5~10kg";
		} else if (val > 10.0 && val <= 15.0) {
			return "10~15kg";
		} else if (val > 15.0 && val <= 20.0) {
			return "15~20kg";
		} else if (val > 20.0 && val <= 25.0) {
			return "20~25kg";
		} else if (val > 25.0 && val <= 30.0) {
			return "25~30kg";
		} else {
			return "30kg 이상";
		}
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

	public boolean isImageSearchAvailable() {
		return imageEmbeddingService.isPresent();
	}

	@Transactional(readOnly = true)
	public List<AnimalImageSearchResult> searchByImage(
			byte[] imageBytes,
			String filename,
			String reportType,
			String animalType,
			String region,
			String keyword,
			String keywordScope,
			java.time.LocalDate startDate,
			java.time.LocalDate endDate,
			String detailPlace
	) {
		if (imageEmbeddingService.isEmpty()) return List.of();
		List<AnimalImageEmbeddingService.ImageSearchHit> hits = imageEmbeddingService.get().searchByImage(imageBytes, filename);
		if (hits.isEmpty()) return List.of();
		List<Long> reportIds = hits.stream().map(AnimalImageEmbeddingService.ImageSearchHit::reportId).toList();

		Specification<AnimalReport> spec = animalReportSearchSpec(reportType, animalType, region, keyword, keywordScope, startDate, endDate, detailPlace);
		Specification<AnimalReport> idSpec = (root, query, cb) -> root.get("reportId").in(reportIds);
		Specification<AnimalReport> combinedSpec = spec.and(idSpec);

		Map<Long, AnimalReport> reportMap = animalReportRepository.findAll(combinedSpec).stream()
				.collect(Collectors.toMap(AnimalReport::getReportId, r -> r));
		Map<Long, String> thumbnails = resolveThumbnails(reportMap.values().stream().toList());

		return hits.stream()
				.filter(hit -> reportMap.containsKey(hit.reportId()))
				.map(hit -> new AnimalImageSearchResult(
						toListView(reportMap.get(hit.reportId()), thumbnails),
						Math.round(hit.score() * 100)
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AnimalImageSearchResult> searchByImage(byte[] imageBytes, String filename) {
		return searchByImage(imageBytes, filename, null, null, null, null, "ALL", null, null, null);
	}

	@Transactional(readOnly = true)
	public List<AnimalMatchCandidateView> getMatchCandidates(Long reportId, String reportType) {
		List<AnimalReportMatch> matches = reportType.equals("MISSING")
				? animalReportMatchRepository.findByMissingReportId(reportId)
				: animalReportMatchRepository.findBySightingReportId(reportId);

		List<AnimalReport> candidates = matches.stream()
				.map(m -> reportType.equals("MISSING") ? m.getSightingReport() : m.getMissingReport())
				.toList();
		Map<Long, String> thumbnails = resolveThumbnails(candidates);

		return matches.stream().map(m -> {
			AnimalReport candidate = reportType.equals("MISSING") ? m.getSightingReport() : m.getMissingReport();
			String imageUrl = thumbnails.getOrDefault(candidate.getReportId(), PLACEHOLDER_IMAGE);
			return new AnimalMatchCandidateView(
					candidate.getReportId(),
					candidate.getReportType(),
					reportTypeLabel(candidate.getReportType()),
					candidate.getTitle(),
					candidate.getAnimalType(),
					animalTypeLabel(candidate.getAnimalType()),
					candidate.getBreedName(),
					candidate.getFurColor(),
					candidate.getRegionName(),
					candidate.getDetailPlace(),
					candidate.getEventDate(),
					candidate.getStatus(),
					statusLabel(candidate.getStatus()),
					m.getFinalScore(),
					imageUrl
			);
		}).toList();
	}

	private record PublicAnimalDetails(
			String shelterName,
			String shelterAddress,
			String authorityName,
			String noticeNo,
			String noticePeriod,
			String ageText
	) {
		static PublicAnimalDetails empty() {
			return new PublicAnimalDetails(null, null, null, null, null, null);
		}

		boolean hasAny() {
			return StringUtils.hasText(shelterName)
					|| StringUtils.hasText(shelterAddress)
					|| StringUtils.hasText(authorityName)
					|| StringUtils.hasText(noticeNo)
					|| StringUtils.hasText(noticePeriod);
		}
	}

	private String getFormReportType(String dbReportType) {
		if ("PROTECTING".equals(dbReportType) || "TRANSFERRED".equals(dbReportType)) {
			return "SIGHTING";
		}
		return dbReportType;
	}

	private String getFormCareStatus(String dbReportType, String dbCareStatus) {
		if ("PROTECTING".equals(dbReportType)) {
			return "PROTECTING";
		}
		if ("TRANSFERRED".equals(dbReportType)) {
			return "TRANSFERRED";
		}
		return dbCareStatus;
	}

	private String getDetailCareStatus(String dbReportType, String dbCareStatus) {
		if ("PROTECTING".equals(dbReportType)) {
			return "PROTECTING";
		}
		if ("TRANSFERRED".equals(dbReportType)) {
			return "TRANSFERRED";
		}
		return dbCareStatus;
	}
}
