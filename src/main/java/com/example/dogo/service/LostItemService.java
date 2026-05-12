package com.example.dogo.service;

import com.example.dogo.dto.LostItemCreateRequest;
import com.example.dogo.dto.LostItemDetailView;
import com.example.dogo.dto.LostItemView;
import com.example.dogo.dto.MatchCandidateView;
import com.example.dogo.entity.LostItem;
import com.example.dogo.entity.LostItemImage;
import com.example.dogo.entity.User;
import com.example.dogo.repository.LostItemImageRepository;
import com.example.dogo.repository.LostItemRepository;
import com.example.dogo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	private static final Logger log = LoggerFactory.getLogger(LostItemService.class);
	private static final String DEV_USER_EMAIL = "dev@dogo.local";
	private static final String PLACEHOLDER_IMAGE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='900' height='600' viewBox='0 0 900 600'%3E%3Crect width='900' height='600' fill='%23e2e8f0'/%3E%3Cpath d='M260 385h380l-92-120-72 82-56-64-160 102z' fill='%2394a3b8'/%3E%3Ccircle cx='326' cy='214' r='46' fill='%23cbd5e1'/%3E%3Ctext x='450' y='490' text-anchor='middle' font-family='Arial' font-size='34' fill='%2364758b'%3ENo image%3C/text%3E%3C/svg%3E";

	private final LostItemRepository lostItemRepository;
	private final LostItemImageRepository lostItemImageRepository;
	private final UserRepository userRepository;
	private final PoliceLostItemDetailEnrichmentService policeLostItemDetailEnrichmentService;
	private final ItemMatchService itemMatchService;
	private final Path lostItemUploadPath;

	public LostItemService(
			LostItemRepository lostItemRepository,
			LostItemImageRepository lostItemImageRepository,
			UserRepository userRepository,
			PoliceLostItemDetailEnrichmentService policeLostItemDetailEnrichmentService,
			ItemMatchService itemMatchService,
			@Value("${file.upload-dir}") String uploadDir
	) {
		this.lostItemRepository = lostItemRepository;
		this.lostItemImageRepository = lostItemImageRepository;
		this.userRepository = userRepository;
		this.policeLostItemDetailEnrichmentService = policeLostItemDetailEnrichmentService;
		this.itemMatchService = itemMatchService;
		this.lostItemUploadPath = Path.of(uploadDir, "lost-items").toAbsolutePath().normalize();
	}

	@Transactional(readOnly = true)
	public List<LostItemView> search(String keyword, String category, String area, String status) {
		return lostItemRepository.search(keyword, category, area, status).stream()
				.map(this::toListView)
				.toList();
	}

	@Transactional(readOnly = true)
	public Page<LostItemView> search(String keyword, String category, String area, String status, Pageable pageable) {
		return lostItemRepository.search(keyword, category, area, status, pageable)
				.map(this::toListView);
	}

	@Transactional(readOnly = true)
	public List<String> getSearchCategoryNames() {
		return lostItemRepository.findActiveCategoryNames();
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
				defaultLostAt(request.getLostAt()),
				blankToNull(request.getLostArea()),
				request.getLostPlace(),
				blankToNull(request.getContact())
		);

		LostItem savedItem = lostItemRepository.save(lostItem);
		saveImages(savedItem, request.getUploadImages());

		try {
			itemMatchService.matchForLostItem(savedItem);
		} catch (Exception exception) {
			log.warn("분실물 매칭 실행 중 오류가 발생했습니다. lostId={}", savedItem.getLostId(), exception);
		}

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
				lostItem.getContent(),
				lostItem.getContact(),
				imageUrl
		);
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
		if (!StringUtils.hasText(request.getCategoryMain())) {
			throw new IllegalArgumentException("카테고리를 선택해주세요.");
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
		return value;
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
