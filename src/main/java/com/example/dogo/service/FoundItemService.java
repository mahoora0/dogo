package com.example.dogo.service;

import com.example.dogo.dto.FoundItemCreateRequest;
import com.example.dogo.dto.FoundItemDetailView;
import com.example.dogo.dto.FoundItemView;
import com.example.dogo.dto.MatchCandidateView;
import com.example.dogo.entity.FoundItem;
import com.example.dogo.entity.FoundItemImage;
import com.example.dogo.entity.User;
import com.example.dogo.repository.FoundItemImageRepository;
import com.example.dogo.repository.FoundItemRepository;
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
public class FoundItemService {

	private static final Logger log = LoggerFactory.getLogger(FoundItemService.class);
	private static final String DEV_USER_EMAIL = "dev@dogo.local";
	private static final String PLACEHOLDER_IMAGE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='900' height='600' viewBox='0 0 900 600'%3E%3Crect width='900' height='600' fill='%23e2e8f0'/%3E%3Cpath d='M260 385h380l-92-120-72 82-56-64-160 102z' fill='%2394a3b8'/%3E%3Ccircle cx='326' cy='214' r='46' fill='%23cbd5e1'/%3E%3Ctext x='450' y='490' text-anchor='middle' font-family='Arial' font-size='34' fill='%2364758b'%3ENo image%3C/text%3E%3C/svg%3E";

	private final FoundItemRepository foundItemRepository;
	private final FoundItemImageRepository foundItemImageRepository;
	private final UserRepository userRepository;
	private final PoliceFoundItemDetailEnrichmentService policeFoundItemDetailEnrichmentService;
	private final ItemMatchService itemMatchService;
	private final Path foundItemUploadPath;

	public FoundItemService(
			FoundItemRepository foundItemRepository,
			FoundItemImageRepository foundItemImageRepository,
			UserRepository userRepository,
			PoliceFoundItemDetailEnrichmentService policeFoundItemDetailEnrichmentService,
			ItemMatchService itemMatchService,
			@Value("${file.upload-dir}") String uploadDir
	) {
		this.foundItemRepository = foundItemRepository;
		this.foundItemImageRepository = foundItemImageRepository;
		this.userRepository = userRepository;
		this.policeFoundItemDetailEnrichmentService = policeFoundItemDetailEnrichmentService;
		this.itemMatchService = itemMatchService;
		this.foundItemUploadPath = Path.of(uploadDir, "found-items").toAbsolutePath().normalize();
	}

	@Transactional(readOnly = true)
	public Page<FoundItemView> search(String keyword, String category, String area, String status, Pageable pageable) {
		return foundItemRepository.search(keyword, category, area, status, pageable)
				.map(this::toListView);
	}

	@Transactional(readOnly = true)
	public List<String> getSearchCategoryNames() {
		return foundItemRepository.findActiveCategoryNames();
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
				imageUrls
		);
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
				blankToNull(request.getFoundArea()),
				blankToNull(request.getFoundPlace()),
				blankToNull(request.getKeepPlace()),
				blankToNull(request.getColorName()),
				blankToNull(request.getContent()),
				null
		);

		FoundItem savedItem = foundItemRepository.save(foundItem);
		saveImages(savedItem, request.getUploadImages());

		try {
			itemMatchService.matchForFoundItem(savedItem);
		} catch (Exception exception) {
			log.warn("습득물 매칭 실행 중 오류가 발생했습니다. foundId={}", savedItem.getFoundId(), exception);
		}

		return savedItem.getFoundId();
	}

	private FoundItemView toListView(FoundItem foundItem) {
		String imageUrl = foundItemImageRepository.findFirstByFoundItemOrderBySortOrderAscImageIdAsc(foundItem)
				.map(FoundItemImage::getImageUrl)
				.orElse(PLACEHOLDER_IMAGE);

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
			String extension = extractExtension(originalName);
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
		if (!StringUtils.hasText(request.getCategoryMain())) {
			throw new IllegalArgumentException("카테고리를 선택해주세요.");
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

	private String statusLabel(String status) {
		return switch (status) {
			case "MATCHING" -> "매칭중";
			case "RETURNED" -> "수령완료";
			default -> "보관중";
		};
	}
}
