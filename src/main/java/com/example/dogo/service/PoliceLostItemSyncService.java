package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemPage;
import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.dto.PoliceLostItemResponse;
import com.example.dogo.dto.PoliceLostItemSyncResult;
import com.example.dogo.entity.LostItem;
import com.example.dogo.entity.LostItemImage;
import com.example.dogo.repository.LostItemImageRepository;
import com.example.dogo.repository.LostItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class PoliceLostItemSyncService {

	private static final Logger log = LoggerFactory.getLogger(PoliceLostItemSyncService.class);

	private final PoliceLostItemClient client;
	private final PoliceLostItemMapper mapper;
	private final LostItemRepository lostItemRepository;
	private final LostItemImageRepository lostItemImageRepository;
	private final int numOfRows;
	private final int incrementalEmptyPageLimit;

	public PoliceLostItemSyncService(
			PoliceLostItemClient client,
			PoliceLostItemMapper mapper,
			LostItemRepository lostItemRepository,
			LostItemImageRepository lostItemImageRepository,
			@Value("${police.lost-item.num-of-rows:100}") int numOfRows,
			@Value("${police.lost-item.incremental-empty-page-limit:2}") int incrementalEmptyPageLimit
	) {
		this.client = client;
		this.mapper = mapper;
		this.lostItemRepository = lostItemRepository;
		this.lostItemImageRepository = lostItemImageRepository;
		this.numOfRows = numOfRows;
		this.incrementalEmptyPageLimit = incrementalEmptyPageLimit;
	}

	public PoliceLostItemSyncResult syncBackfillLastMonth() {
		LocalDate endDate = LocalDate.now();
		return syncBackfill(endDate.minusMonths(1), endDate);
	}

	public PoliceLostItemSyncResult syncIncrementalLastMonth() {
		LocalDate endDate = LocalDate.now();
		return syncIncremental(endDate.minusMonths(1), endDate);
	}

	PoliceLostItemSyncResult syncBackfill(LocalDate startDate, LocalDate endDate) {
		return sync(startDate, endDate, false);
	}

	PoliceLostItemSyncResult syncIncremental(LocalDate startDate, LocalDate endDate) {
		return sync(startDate, endDate, true);
	}

	private PoliceLostItemSyncResult sync(LocalDate startDate, LocalDate endDate, boolean stopAfterDuplicatePages) {
		int pageNo = 1;
		int emptyNewPageCount = 0;
		int fetchedCount = 0;
		int savedCount = 0;
		int skippedCount = 0;
		int pageCount = 0;

		while (true) {
			PoliceLostItemPage page = client.fetchLostItems(startDate, endDate, pageNo, numOfRows);
			pageCount++;

			if (page.items().isEmpty()) {
				break;
			}

			int newCount = 0;
			for (PoliceLostItemResponse response : page.items()) {
				fetchedCount++;
				if (saveIfNew(response)) {
					savedCount++;
					newCount++;
				} else {
					skippedCount++;
				}
			}

			if (stopAfterDuplicatePages) {
				if (newCount == 0) {
					emptyNewPageCount++;
				} else {
					emptyNewPageCount = 0;
				}

				if (emptyNewPageCount >= incrementalEmptyPageLimit) {
					break;
				}
			}

			pageNo++;
		}

		return new PoliceLostItemSyncResult(fetchedCount, savedCount, skippedCount, pageCount);
	}

	private boolean saveIfNew(PoliceLostItemResponse response) {
		if (response == null || !StringUtils.hasText(response.atcId())) {
			return false;
		}

		String atcId = response.atcId().trim();
		if (lostItemRepository.existsByAtcId(atcId)) {
			return false;
		}

		try {
			Optional<PoliceLostItemDetailResponse> detail = fetchDetail(atcId);
			LostItem lostItem = mapper.toLostItem(response, detail.orElse(null));
			LostItem savedItem = lostItemRepository.save(lostItem);
			detail.ifPresent(detailResponse -> saveImageIfPresent(savedItem, detailResponse));
			return true;
		} catch (DataIntegrityViolationException exception) {
			log.debug("이미 저장된 경찰청 분실물입니다. atcId={}", atcId, exception);
			return false;
		} catch (IllegalArgumentException exception) {
			log.warn("경찰청 분실물 응답을 저장하지 못했습니다. atcId={}, reason={}", atcId, exception.getMessage());
			return false;
		}
	}

	private Optional<PoliceLostItemDetailResponse> fetchDetail(String atcId) {
		try {
			return client.fetchLostItemDetail(atcId);
		} catch (RuntimeException exception) {
			log.warn("경찰청 분실물 상세 조회에 실패했습니다. 목록 정보만 저장합니다. atcId={}", atcId, exception);
			return Optional.empty();
		}
	}

	private void saveImageIfPresent(LostItem lostItem, PoliceLostItemDetailResponse detail) {
		String imageUrl = detail.lstFilePathImg();
		if (!isActualImageUrl(imageUrl)) {
			return;
		}

		lostItemImageRepository.save(new LostItemImage(
				lostItem,
				originalName(imageUrl),
				lostItem.getAtcId(),
				imageUrl.trim(),
				"image/external",
				null,
				0
		));
	}

	private boolean isActualImageUrl(String imageUrl) {
		if (!StringUtils.hasText(imageUrl)) {
			return false;
		}

		String normalized = imageUrl.trim().toLowerCase();
		return normalized.startsWith("http")
				&& !normalized.contains("no_img")
				&& !normalized.contains("noimage");
	}

	private String originalName(String imageUrl) {
		try {
			String path = new URI(imageUrl.trim()).getPath();
			if (StringUtils.hasText(path) && path.contains("/")) {
				String filename = path.substring(path.lastIndexOf('/') + 1);
				if (StringUtils.hasText(filename)) {
					return filename;
				}
			}
		} catch (URISyntaxException ignored) {
			return "police-image";
		}
		return "police-image";
	}
}
