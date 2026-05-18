package com.example.dogo.service.item;

import com.example.dogo.dto.item.LostItemCreateRequest;
import com.example.dogo.dto.item.LostItemDetailView;
import com.example.dogo.dto.item.LostItemView;
import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.dto.item.RecentItemView;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.item.LostItemImage;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.item.LostItemImageRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.service.match.ItemMatchService;
import com.example.dogo.service.match.LostItemMatchRequestedEvent;
import com.example.dogo.service.match.embedding.LostItemEmbeddingRequestedEvent;
import com.example.dogo.service.police.sync.PoliceLostItemDetailEnrichmentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LostItemService {

	private static final String DEV_USER_EMAIL = "dev@dogo.local";
	private static final String PLACEHOLDER_IMAGE = "/images/noImageSize.png";

	private final LostItemRepository lostItemRepository;
	private final LostItemImageRepository lostItemImageRepository;
	private final UserRepository userRepository;
	private final PoliceLostItemDetailEnrichmentService policeLostItemDetailEnrichmentService;
	private final ItemMatchService itemMatchService;
	private final ApplicationEventPublisher eventPublisher;
	private final Path lostItemUploadPath;

	public LostItemService(
			LostItemRepository lostItemRepository,
			LostItemImageRepository lostItemImageRepository,
			UserRepository userRepository,
			PoliceLostItemDetailEnrichmentService policeLostItemDetailEnrichmentService,
			ItemMatchService itemMatchService,
			ApplicationEventPublisher eventPublisher,
			@Value("${file.upload-dir}") String uploadDir
	) {
		this.lostItemRepository = lostItemRepository;
		this.lostItemImageRepository = lostItemImageRepository;
		this.userRepository = userRepository;
		this.policeLostItemDetailEnrichmentService = policeLostItemDetailEnrichmentService;
		this.itemMatchService = itemMatchService;
		this.eventPublisher = eventPublisher;
		this.lostItemUploadPath = Path.of(uploadDir, "lost-items").toAbsolutePath().normalize();
	}

	@Transactional(readOnly = true)
	public List<LostItemView> search(String keyword, String category, String area, String status) {
		return lostItemRepository.findAll(
						lostItemSearchSpec(keyword, "ALL", category, area, status),
						Sort.by(Sort.Direction.DESC, "lostAt").and(Sort.by(Sort.Direction.DESC, "lostId"))
				).stream()
				.map(this::toListView)
				.toList();
	}

	@Transactional(readOnly = true)
	public Page<LostItemView> search(String keyword, String category, String area, String status, Pageable pageable) {
		return search(keyword, "ALL", category, area, status, pageable);
	}

	@Transactional(readOnly = true)
	public Page<LostItemView> search(
			String keyword,
			String keywordScope,
			String category,
			String area,
			String status,
			Pageable pageable
	) {
		return lostItemRepository.findAll(lostItemSearchSpec(keyword, keywordScope, category, area, status), pageable)
				.map(this::toListView);
	}

	@Transactional(readOnly = true)
	public List<RecentItemView> getRecentItems(int limit) {
		Pageable pageable = Pageable.ofSize(limit);
		return lostItemRepository.findByDeletedFalseOrderByLostAtDescLostIdDesc(pageable).stream()
				.map(item -> new RecentItemView(
						item.getLostId(),
						"LOST",
						"분실",
						item.getTitle(),
						categoryName(item),
						item.getLostPlace(),
						item.getLostAt(),
						item.getStatus(),
						statusLabel(item.getStatus()),
						lostItemImageRepository.findFirstByLostItemOrderBySortOrderAscImageIdAsc(item)
								.map(LostItemImage::getImageUrl)
								.orElse(PLACEHOLDER_IMAGE)
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<String> getSearchCategoryNames() {
		return lostItemRepository.findActiveCategoryNames();
	}

	private Specification<LostItem> lostItemSearchSpec(
			String keyword,
			String keywordScope,
			String category,
			String area,
			String status
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
						criteriaBuilder.lower(root.get("lostArea")),
						"%" + normalizedArea.toLowerCase(Locale.ROOT) + "%"
				));
			}

			String normalizedStatus = blankToNull(status);
			if (normalizedStatus != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), normalizedStatus));
			}

			String normalizedKeyword = blankToNull(keyword);
			if (normalizedKeyword != null) {
				String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
				List<jakarta.persistence.criteria.Predicate> keywordPredicates = lostKeywordFields(keywordScope).stream()
						.map(field -> criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), pattern))
						.toList();
				predicates.add(criteriaBuilder.or(keywordPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
			}

			return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	private List<String> lostKeywordFields(String keywordScope) {
		return switch (defaultText(keywordScope, "ALL").trim()) {
			case "TITLE_PLACE" -> List.of("title", "lostArea", "lostPlace");
			case "ITEM_CATEGORY" -> List.of("itemName", "categoryMain", "categorySub");
			case "CONTENT" -> List.of("content");
			case "COLOR" -> List.of("colorName");
			default -> List.of("title", "itemName", "categoryMain", "categorySub", "colorName", "content", "lostArea", "lostPlace");
		};
	}

	@Transactional(readOnly = true)
	public boolean hasLostItems(User user) {
		if (user == null) return false;
		return lostItemRepository.existsByUserAndDeletedFalse(user);
	}

	@Transactional(readOnly = true)
	public List<MatchCandidateView> getMatchCandidates(Long lostId) {
		return itemMatchService.getMatchesForLostItem(lostId);
	}

	@Transactional
	public LostItemDetailView getDetail(Long id) {
		LostItem lostItem = lostItemRepository.findById(id)
				.filter(item -> !item.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("분실물 게시글을 찾을 수 없습니다."));

		policeLostItemDetailEnrichmentService.enrichIfNeeded(lostItem);

		List<String> imageUrls = lostItemImageRepository.findByLostItemOrderBySortOrderAscImageIdAsc(lostItem).stream()
				.map(LostItemImage::getImageUrl)
				.toList();

		if (imageUrls.isEmpty()) {
			imageUrls = List.of(PLACEHOLDER_IMAGE);
		}

		return new LostItemDetailView(
				lostItem.getLostId(),
				lostItem.getTitle(),
				lostItem.getItemName(),
				categoryName(lostItem),
				lostItem.getLostArea(),
				lostItem.getLostPlace(),
				lostItem.getLostAt(),
				lostItem.getStatus(),
				statusLabel(lostItem.getStatus()),
				colorName(lostItem),
				lostItem.getContent(),
				lostItem.getContact(),
				imageUrls
		);
	}

	@Transactional
	public Long create(LostItemCreateRequest request, User loginUser) {
		validateCreateRequest(request);

		User user = (loginUser != null) ? loginUser : getOrCreateDevUser();
		LostItem lostItem = new LostItem(
				user,
				defaultText(request.getTitle(), request.getItemName()),
				blankToNull(request.getContent()),
				request.getItemName(),
				request.getCategoryMain(),
				blankToNull(request.getCategorySub()),
				blankToNull(request.getColorName()),
				defaultLostAt(request.getLostAt()),
				normalizedArea(request.getLostAreaProvince(), request.getLostAreaDistrict(), request.getLostArea()),
				request.getLostPlace(),
				blankToNull(request.getContact())
		);

		LostItem savedItem = lostItemRepository.save(lostItem);
		saveImages(savedItem, request.getUploadImages());
		eventPublisher.publishEvent(new LostItemEmbeddingRequestedEvent(savedItem.getLostId()));
		eventPublisher.publishEvent(new LostItemMatchRequestedEvent(savedItem.getLostId()));

		return savedItem.getLostId();
	}

	private LostItemView toListView(LostItem lostItem) {
		String imageUrl = lostItemImageRepository.findFirstByLostItemOrderBySortOrderAscImageIdAsc(lostItem)
				.map(LostItemImage::getImageUrl)
				.orElse(PLACEHOLDER_IMAGE);

		return new LostItemView(
				lostItem.getLostId(),
				lostItem.getTitle(),
				lostItem.getItemName(),
				categoryName(lostItem),
				lostItem.getLostArea(),
				lostItem.getLostPlace(),
				lostItem.getLostAt(),
				lostItem.getStatus(),
				statusLabel(lostItem.getStatus()),
				colorName(lostItem),
				lostItem.getContent(),
				lostItem.getContact(),
				imageUrl
		);
	}

	private String colorName(LostItem lostItem) {
		if (StringUtils.hasText(lostItem.getColorName())) {
			return lostItem.getColorName();
		}

		String text = String.join(" ",
				defaultText(lostItem.getItemName(), ""),
				defaultText(lostItem.getTitle(), ""),
				defaultText(lostItem.getContent(), "")
		).toLowerCase(Locale.ROOT);

		if (text.contains("블랙") || text.contains("검정") || text.contains("검은") || text.contains("black")) {
			return "블랙(검정)";
		}
		if (text.contains("화이트") || text.contains("흰색") || text.contains("하얀") || text.contains("white")) {
			return "화이트(흰색)";
		}
		if (text.contains("레드") || text.contains("빨강") || text.contains("빨간") || text.contains("red")) {
			return "레드(빨강)";
		}
		if (text.contains("블루") || text.contains("파랑") || text.contains("파란") || text.contains("blue")) {
			return "블루(파랑)";
		}
		if (text.contains("그린") || text.contains("초록") || text.contains("green")) {
			return "그린(초록)";
		}
		if (text.contains("옐로") || text.contains("노랑") || text.contains("노란") || text.contains("yellow")) {
			return "옐로우(노랑)";
		}
		if (text.contains("핑크") || text.contains("분홍") || text.contains("pink")) {
			return "핑크(분홍)";
		}
		if (text.contains("브라운") || text.contains("갈색") || text.contains("brown")) {
			return "브라운(갈색)";
		}
		if (text.contains("그레이") || text.contains("회색") || text.contains("gray") || text.contains("grey")) {
			return "그레이(회색)";
		}
		if (text.contains("베이지") || text.contains("beige")) {
			return "베이지";
		}
		if (text.contains("실버") || text.contains("은색") || text.contains("silver")) {
			return "실버(은색)";
		}
		if (text.contains("골드") || text.contains("금색") || text.contains("gold")) {
			return "골드(금색)";
		}
		return null;
	}

	private void saveImages(LostItem lostItem, List<MultipartFile> images) {
		for (int index = 0; index < images.size(); index++) {
			saveImageIfPresent(lostItem, images.get(index), index);
		}
	}

	private void saveImageIfPresent(LostItem lostItem, MultipartFile image, int sortOrder) {
		if (image == null || image.isEmpty()) {
			return;
		}

		try {
			Files.createDirectories(lostItemUploadPath);

			String originalName = StringUtils.cleanPath(String.valueOf(image.getOriginalFilename()));
			String extension = extractExtension(originalName);
			String storedName = UUID.randomUUID() + extension;
			Path targetPath = lostItemUploadPath.resolve(storedName).normalize();
			if (!targetPath.startsWith(lostItemUploadPath)) {
				throw new IllegalArgumentException("올바르지 않은 이미지 파일명입니다.");
			}

			image.transferTo(targetPath);

			lostItemImageRepository.save(new LostItemImage(
					lostItem,
					originalName,
					storedName,
					"/uploads/lost-items/" + storedName,
					image.getContentType(),
					image.getSize(),
					sortOrder
			));
		} catch (IOException exception) {
			throw new UncheckedIOException("이미지 저장에 실패했습니다.", exception);
		}
	}

	private void validateCreateRequest(LostItemCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("분실물 정보를 입력해주세요.");
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
		if (request.getLostAt() == null) {
			throw new IllegalArgumentException("분실 일시를 입력해주세요.");
		}
		if (!StringUtils.hasText(normalizedArea(request.getLostAreaProvince(), request.getLostAreaDistrict(), request.getLostArea()))) {
			throw new IllegalArgumentException("분실 지역을 선택해주세요.");
		}
		if (!StringUtils.hasText(request.getLostPlace())) {
			throw new IllegalArgumentException("분실 장소를 입력해주세요.");
		}
	}

	private User getOrCreateDevUser() {
		return userRepository.findByEmail(DEV_USER_EMAIL)
				.orElseGet(() -> userRepository.save(new User(DEV_USER_EMAIL, "개발용 사용자", "010-0000-0000")));
	}

	private LocalDateTime defaultLostAt(LocalDateTime lostAt) {
		if (lostAt == null) {
			return LocalDateTime.now();
		}
		return lostAt;
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

	private String normalizedArea(String province, String district, String fallback) {
		if (StringUtils.hasText(province) && StringUtils.hasText(district)) {
			return province.trim() + " " + district.trim();
		}
		if (StringUtils.hasText(province)) {
			return province.trim();
		}
		return blankToNull(fallback);
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

	private String categoryName(LostItem lostItem) {
		if (!StringUtils.hasText(lostItem.getCategoryMain())) {
			return null;
		}
		if (StringUtils.hasText(lostItem.getCategorySub())) {
			return lostItem.getCategoryMain().trim() + " > " + lostItem.getCategorySub().trim();
		}
		return lostItem.getCategoryMain().trim();
	}

	private String statusLabel(String status) {
		return switch (status) {
			case "MATCHING" -> "매칭중";
			case "FOUND" -> "회수완료";
			default -> "대기중";
		};
	}
}
