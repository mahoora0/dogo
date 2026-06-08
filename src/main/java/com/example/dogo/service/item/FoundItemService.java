package com.example.dogo.service.item;

import com.example.dogo.dto.item.FoundItemCreateRequest;
import com.example.dogo.dto.item.FoundItemDetailView;
import com.example.dogo.dto.item.FoundItemEditData;
import com.example.dogo.dto.item.FoundItemView;
import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.dto.item.RecentItemView;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.FoundItemImage;
import com.example.dogo.entity.user.User;
import com.example.dogo.service.upload.UploadFileValidator;
import com.example.dogo.repository.item.FoundItemImageRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.service.match.FoundItemMatchRequestedEvent;
import com.example.dogo.service.match.ItemMatchService;
import com.example.dogo.service.police.sync.PoliceFoundItemDetailEnrichmentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FoundItemService {

	private static final String DEV_USER_EMAIL = "dev@dogo.local";
	private static final String PLACEHOLDER_IMAGE = "/images/noImageSize.png";

	private final FoundItemRepository foundItemRepository;
	private final FoundItemImageRepository foundItemImageRepository;
	private final UserRepository userRepository;
	private final PoliceFoundItemDetailEnrichmentService policeFoundItemDetailEnrichmentService;
	private final ItemMatchService itemMatchService;
	private final ApplicationEventPublisher eventPublisher;
	private final Path foundItemUploadPath;

	public FoundItemService(
			FoundItemRepository foundItemRepository,
			FoundItemImageRepository foundItemImageRepository,
			UserRepository userRepository,
			PoliceFoundItemDetailEnrichmentService policeFoundItemDetailEnrichmentService,
			ItemMatchService itemMatchService,
			ApplicationEventPublisher eventPublisher,
			@Value("${file.upload-dir}") String uploadDir
	) {
		this.foundItemRepository = foundItemRepository;
		this.foundItemImageRepository = foundItemImageRepository;
		this.userRepository = userRepository;
		this.policeFoundItemDetailEnrichmentService = policeFoundItemDetailEnrichmentService;
		this.itemMatchService = itemMatchService;
		this.eventPublisher = eventPublisher;
		this.foundItemUploadPath = Path.of(uploadDir, "found-items").toAbsolutePath().normalize();
	}

	@Transactional(readOnly = true)
	public Page<FoundItemView> search(String keyword, String category, String area, String status, Pageable pageable) {
		return search(keyword, "ALL", category, area, status, null, null, pageable);
	}

	@Transactional(readOnly = true)
	public Page<FoundItemView> search(
			String keyword,
			String keywordScope,
			String category,
			String area,
			String status,
			Pageable pageable
	) {
		return search(keyword, keywordScope, category, area, status, null, null, pageable);
	}

	@Transactional(readOnly = true)
	public Page<FoundItemView> search(
			String keyword,
			String keywordScope,
			String category,
			String area,
			String status,
			java.time.LocalDate startDate,
			String detailPlace,
			Pageable pageable
	) {
		Page<FoundItem> page = foundItemRepository.findAll(
				foundItemSearchSpec(keyword, keywordScope, category, area, status, startDate, detailPlace), pageable);
		Map<Long, String> thumbnails = resolveThumbnails(page.getContent());
		return page.map(item -> toListView(item, thumbnails));
	}

	@Transactional(readOnly = true)
	public List<RecentItemView> getRecentItems(int limit) {
		Pageable pageable = Pageable.ofSize(limit);
		List<FoundItem> items = foundItemRepository.findByDeletedFalseOrderByRegDateDescFoundIdDesc(pageable);
		Map<Long, String> thumbnails = resolveThumbnails(items);
		return items.stream()
				.map(item -> new RecentItemView(
						item.getFoundId(),
						"FOUND",
						"습득",
						item.getTitle(),
						item.getCategoryMain(),
						item.getFoundPlace() != null ? item.getFoundPlace() : item.getFoundArea(),
						item.getFoundAt(),
						item.getStatus(),
						statusLabel(item.getStatus()),
						thumbnails.getOrDefault(item.getFoundId(), PLACEHOLDER_IMAGE),
						item.getRegDate()
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<String> getSearchCategoryNames() {
		return foundItemRepository.findActiveCategoryNames();
	}

	private Specification<FoundItem> foundItemSearchSpec(
			String keyword,
			String keywordScope,
			String category,
			String area,
			String status,
			java.time.LocalDate startDate,
			String detailPlace
	) {
		return (root, query, criteriaBuilder) -> {
			List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
			predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

			String normalizedCategory = blankToNull(category);
			if (normalizedCategory != null) {
				predicates.add(criteriaBuilder.equal(root.get("categoryMain"), normalizedCategory));
			}

			String normalizedArea = blankToNull(area);
			if (normalizedArea != null) {
				predicates.add(criteriaBuilder.like(
						criteriaBuilder.lower(root.get("foundArea")),
						"%" + normalizedArea.toLowerCase(Locale.ROOT) + "%"
				));
			}

			String normalizedStatus = blankToNull(status);
			if (normalizedStatus != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), normalizedStatus));
			}

			if (startDate != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("foundAt"), startDate.atStartOfDay()));
			}

			String normalizedDetailPlace = blankToNull(detailPlace);
			if (normalizedDetailPlace != null) {
				predicates.add(criteriaBuilder.like(
						criteriaBuilder.lower(root.get("foundPlace")),
						"%" + normalizedDetailPlace.toLowerCase(Locale.ROOT) + "%"
				));
			}

			String normalizedKeyword = blankToNull(keyword);
			if (normalizedKeyword != null) {
				String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
				List<jakarta.persistence.criteria.Predicate> keywordPredicates = foundKeywordFields(keywordScope).stream()
						.map(field -> criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), pattern))
						.toList();
				predicates.add(criteriaBuilder.or(keywordPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
			}

			return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	private List<String> foundKeywordFields(String keywordScope) {
		return switch (defaultText(keywordScope, "ALL").trim()) {
			case "TITLE_PLACE" -> List.of("title", "foundArea", "foundPlace", "keepPlace");
			case "ITEM_CATEGORY" -> List.of("itemName", "categoryMain", "categorySub");
			case "CONTENT" -> List.of("content");
			case "COLOR" -> List.of("colorName");
			default -> List.of("title", "itemName", "categoryMain", "categorySub", "colorName", "content", "foundArea", "foundPlace", "keepPlace");
		};
	}

	@Transactional(readOnly = true)
	public List<MatchCandidateView> getMatchCandidates(Long foundId) {
		return itemMatchService.getMatchesForFoundItem(foundId);
	}

	@Transactional(readOnly = true)
	public FoundItemDetailView getDetail(Long id) {
		FoundItem foundItem = foundItemRepository.findById(id)
				.filter(item -> !item.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("습득물 게시글을 찾을 수 없습니다."));

		policeFoundItemDetailEnrichmentService.enrichIfNeeded(foundItem);

		List<String> imageUrls = foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(foundItem).stream()
				.map(FoundItemImage::getImageUrl)
				.toList();

		if (imageUrls.isEmpty()) {
			imageUrls = List.of(PLACEHOLDER_IMAGE);
		}

		return new FoundItemDetailView(
				foundItem.getFoundId(),
				foundItem.getTitle(),
				foundItem.getItemName(),
				foundItem.getCategoryMain(),
				foundItem.getFoundArea(),
				foundItem.getFoundPlace(),
				foundItem.getKeepPlace(),
				foundItem.getFoundAt(),
				foundItem.getStatus(),
				statusLabel(foundItem.getStatus()),
				foundItem.getContent(),
				foundItem.getContact(),
				foundItem.getColorName(),
				imageUrls,
				foundItem.getUser() != null ? foundItem.getUser().getUserNo() : null
		);
	}

	@Transactional(readOnly = true)
	public FoundItemEditData getForEdit(Long id, User loginUser) {
		FoundItem item = foundItemRepository.findById(id)
				.filter(i -> !i.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("습득물 게시글을 찾을 수 없습니다."));
		checkOwnership(item, loginUser);

		List<String> imageUrls = foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(item).stream()
				.map(FoundItemImage::getImageUrl)
				.toList();

		String area = defaultText(item.getFoundArea(), "");
		String[] parts = area.split(" ", 2);
		String province = parts.length > 0 ? parts[0] : "";
		String district = parts.length > 1 ? parts[1] : "";

		return new FoundItemEditData(
				item.getFoundId(),
				item.getTitle(),
				item.getItemName(),
				item.getCategoryMain(),
				item.getCategorySub(),
				item.getColorName(),
				item.getFoundAt(),
				province,
				district,
				item.getFoundPlace(),
				item.getKeepPlace(),
				item.getContent(),
				imageUrls
		);
	}

	@Transactional
	public void update(Long id, FoundItemCreateRequest request, User loginUser) {
		validateCreateRequest(request);
		FoundItem item = foundItemRepository.findById(id)
				.filter(i -> !i.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("습득물 게시글을 찾을 수 없습니다."));
		checkOwnership(item, loginUser);

		item.update(
				defaultText(request.getTitle(), request.getItemName()),
				blankToNull(request.getContent()),
				request.getItemName(),
				request.getCategoryMain(),
				blankToNull(request.getCategorySub()),
				blankToNull(request.getColorName()),
				defaultFoundAt(request.getFoundAt()),
				normalizedArea(request.getFoundAreaProvince(), request.getFoundAreaDistrict(), request.getFoundArea()),
				blankToNull(request.getFoundPlace()),
				blankToNull(request.getKeepPlace())
		);

		List<MultipartFile> newImages = request.getUploadImages();
		if (!newImages.isEmpty()) {
			List<FoundItemImage> oldImages = foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(item);
			for (FoundItemImage old : oldImages) {
				try {
					Files.deleteIfExists(foundItemUploadPath.resolve(old.getStoredName()));
				} catch (IOException ignored) {
				}
			}
			foundItemImageRepository.deleteAll(oldImages);
			saveImages(item, newImages);
		}

		eventPublisher.publishEvent(new FoundItemMatchRequestedEvent(item.getFoundId()));
	}

	@Transactional
	public void updateStatus(Long id, String status, User loginUser) {
		FoundItem item = foundItemRepository.findById(id)
				.filter(i -> !i.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("습득물 게시글을 찾을 수 없습니다."));
		checkOwnership(item, loginUser);
		item.setStatus(normalizeStatus(status));
	}

	@Transactional
	public void delete(Long id, User loginUser) {
		FoundItem item = foundItemRepository.findById(id)
				.filter(i -> !i.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("습득물 게시글을 찾을 수 없습니다."));
		checkOwnership(item, loginUser);

		itemMatchService.clearMatchesForFoundItem(item.getFoundId());
		item.markDeleted();
	}

	private void checkOwnership(FoundItem item, User loginUser) {
		if (!"USER".equals(item.getSourceType())) {
			throw new IllegalArgumentException("수정 권한이 없습니다.");
		}
		if (loginUser == null || item.getUser() == null || !item.getUser().getUserNo().equals(loginUser.getUserNo())) {
			throw new IllegalArgumentException("수정 권한이 없습니다.");
		}
	}

	@Transactional
	public Long create(FoundItemCreateRequest request, User loginUser) {
		validateCreateRequest(request);

		User user = (loginUser != null) ? loginUser : getOrCreateDevUser();
		FoundItem foundItem = new FoundItem(
				user,
				defaultText(request.getTitle(), request.getItemName()),
				request.getItemName(),
				request.getCategoryMain(),
				blankToNull(request.getCategorySub()),
				defaultFoundAt(request.getFoundAt()),
				normalizedArea(request.getFoundAreaProvince(), request.getFoundAreaDistrict(), request.getFoundArea()),
				blankToNull(request.getFoundPlace()),
				blankToNull(request.getKeepPlace()),
				blankToNull(request.getColorName()),
				blankToNull(request.getContent()),
				null
		);

		FoundItem savedItem = foundItemRepository.save(foundItem);
		saveImages(savedItem, request.getUploadImages());
		eventPublisher.publishEvent(new FoundItemMatchRequestedEvent(savedItem.getFoundId()));

		return savedItem.getFoundId();
	}

	private Map<Long, String> resolveThumbnails(List<FoundItem> items) {
		if (items.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> thumbnails = new HashMap<>();
		for (FoundItemImage image : foundItemImageRepository.findByFoundItemInOrderBySortOrderAscImageIdAsc(items)) {
			thumbnails.putIfAbsent(image.getFoundItem().getFoundId(), image.getImageUrl());
		}
		return thumbnails;
	}

	private FoundItemView toListView(FoundItem foundItem, Map<Long, String> thumbnails) {
		String imageUrl = thumbnails.getOrDefault(foundItem.getFoundId(), PLACEHOLDER_IMAGE);

		return new FoundItemView(
				foundItem.getFoundId(),
				foundItem.getTitle(),
				foundItem.getItemName(),
				foundItem.getCategoryMain(),
				foundItem.getFoundArea(),
				foundItem.getFoundPlace(),
				foundItem.getKeepPlace(),
				foundItem.getFoundAt(),
				foundItem.getStatus(),
				statusLabel(foundItem.getStatus()),
				foundItem.getColorName(),
				imageUrl
		);
	}

	private void saveImages(FoundItem foundItem, List<MultipartFile> images) {
		for (int index = 0; index < images.size(); index++) {
			saveImageIfPresent(foundItem, images.get(index), index);
		}
	}

	private void saveImageIfPresent(FoundItem foundItem, MultipartFile image, int sortOrder) {
		if (image == null || image.isEmpty()) {
			return;
		}

		try {
			Files.createDirectories(foundItemUploadPath);

			String originalName = StringUtils.cleanPath(String.valueOf(image.getOriginalFilename()));
			String extension = UploadFileValidator.imageExtension(image);
			String storedName = UUID.randomUUID() + extension;
			Path targetPath = foundItemUploadPath.resolve(storedName).normalize();
			if (!targetPath.startsWith(foundItemUploadPath)) {
				throw new IllegalArgumentException("올바르지 않은 이미지 파일명입니다.");
			}

			image.transferTo(targetPath);

			foundItemImageRepository.save(new FoundItemImage(
					foundItem,
					originalName,
					storedName,
					"/uploads/found-items/" + storedName,
					image.getContentType(),
					image.getSize(),
					sortOrder
			));
		} catch (IOException exception) {
			throw new UncheckedIOException("이미지 저장에 실패했습니다.", exception);
		}
	}

	private void validateCreateRequest(FoundItemCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("습득물 정보를 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getItemName())) {
			throw new IllegalArgumentException("물품명을 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getTitle())) {
			throw new IllegalArgumentException("제목을 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getCategoryMain())) {
			throw new IllegalArgumentException("카테고리를 선택해주세요.");
		}
		if (request.getFoundAt() == null) {
			throw new IllegalArgumentException("습득 일시를 입력해주세요.");
		}
		if (!StringUtils.hasText(normalizedArea(request.getFoundAreaProvince(), request.getFoundAreaDistrict(), request.getFoundArea()))) {
			throw new IllegalArgumentException("습득 지역을 선택해주세요.");
		}
		if (!StringUtils.hasText(request.getFoundPlace())) {
			throw new IllegalArgumentException("습득 장소를 입력해주세요.");
		}
	}

	private User getOrCreateDevUser() {
		return userRepository.findByEmail(DEV_USER_EMAIL)
				.orElseGet(() -> userRepository.save(new User(DEV_USER_EMAIL, "개발용 사용자", "010-0000-0000")));
	}

	private LocalDateTime defaultFoundAt(LocalDateTime foundAt) {
		if (foundAt == null) {
			return LocalDateTime.now();
		}
		return foundAt;
	}

	private String defaultText(String value, String fallback) {
		if (StringUtils.hasText(value)) {
			return value;
		}
		return fallback;
	}

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private String normalizeStatus(String status) {
		String normalizedStatus = blankToNull(status);
		if ("KEEPING".equals(normalizedStatus) || "RETURNED".equals(normalizedStatus) || "TRANSFERRED".equals(normalizedStatus)) {
			return normalizedStatus;
		}
		throw new IllegalArgumentException("변경할 수 없는 상태입니다.");
	}

	private String normalizedArea(String province, String district, String fallback) {
		if (StringUtils.hasText(province) && StringUtils.hasText(district)) {
			return province.trim() + " " + district.trim();
		}
		if (StringUtils.hasText(province)) {
			return province.trim();
		}
		return blankToNull(fallback);
	}

	private String statusLabel(String status) {
		return switch (status) {
			case "RETURNED" -> "반환";
			case "TRANSFERRED" -> "연계";
			default -> "보관";
		};
	}
}
