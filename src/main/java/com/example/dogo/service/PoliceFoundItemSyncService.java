package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.PoliceFoundItemPage;
import com.example.dogo.dto.PoliceFoundItemResponse;
import com.example.dogo.dto.PoliceFoundItemSyncResult;
import com.example.dogo.entity.FoundItem;
import com.example.dogo.repository.FoundItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PoliceFoundItemSyncService {

	private static final Logger log = LoggerFactory.getLogger(PoliceFoundItemSyncService.class);

	private final PoliceFoundItemClient client;
	private final PoliceFoundItemMapper mapper;
	private final FoundItemRepository foundItemRepository;
	private final PoliceFoundItemImageService imageService;
	private final int numOfRows;
	private final int incrementalEmptyPageLimit;
	private final int backfillLookbackDays;
	private final int incrementalLookbackDays;

	public PoliceFoundItemSyncService(
			PoliceFoundItemClient client,
			PoliceFoundItemMapper mapper,
			FoundItemRepository foundItemRepository,
			PoliceFoundItemImageService imageService,
			@Value("${police.found-item.num-of-rows:100}") int numOfRows,
			@Value("${police.found-item.incremental-empty-page-limit:2}") int incrementalEmptyPageLimit,
			@Value("${police.found-item.backfill-lookback-days:1}") int backfillLookbackDays,
			@Value("${police.found-item.incremental-lookback-days:30}") int incrementalLookbackDays
	) {
		this.client = client;
		this.mapper = mapper;
		this.foundItemRepository = foundItemRepository;
		this.imageService = imageService;
		this.numOfRows = numOfRows;
		this.incrementalEmptyPageLimit = incrementalEmptyPageLimit;
		this.backfillLookbackDays = Math.max(backfillLookbackDays, 1);
		this.incrementalLookbackDays = Math.max(incrementalLookbackDays, 1);
	}

	public PoliceFoundItemSyncResult syncBackfillLastMonth() {
		LocalDate endDate = LocalDate.now();
		return syncBackfill(endDate.minusDays(backfillLookbackDays), endDate);
	}

	public PoliceFoundItemSyncResult syncIncrementalLastMonth() {
		LocalDate endDate = LocalDate.now();
		return syncIncremental(endDate.minusDays(incrementalLookbackDays), endDate);
	}

	PoliceFoundItemSyncResult syncBackfill(LocalDate startDate, LocalDate endDate) {
		return sync(startDate, endDate, false, true);
	}

	PoliceFoundItemSyncResult syncIncremental(LocalDate startDate, LocalDate endDate) {
		return sync(startDate, endDate, true, true);
	}

	private PoliceFoundItemSyncResult sync(
			LocalDate startDate,
			LocalDate endDate,
			boolean stopAfterDuplicatePages,
			boolean includeDetail
	) {
		int pageNo = 1;
		int emptyNewPageCount = 0;
		int fetchedCount = 0;
		int savedCount = 0;
		int skippedCount = 0;
		int pageCount = 0;

		while (true) {
			PoliceFoundItemPage page = client.fetchFoundItems(startDate, endDate, pageNo, numOfRows);
			pageCount++;

			if (page.items().isEmpty()) {
				break;
			}

			int newCount = 0;
			for (PoliceFoundItemResponse response : page.items()) {
				fetchedCount++;
				if (saveIfNew(response, includeDetail)) {
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

		return new PoliceFoundItemSyncResult(fetchedCount, savedCount, skippedCount, pageCount);
	}

	private boolean saveIfNew(PoliceFoundItemResponse response, boolean includeDetail) {
		if (response == null || !StringUtils.hasText(response.atcId())) {
			return false;
		}

		String atcId = response.atcId().trim();
		Integer fdSn = mapper.parseOptionalFdSn(response.fdSn());
		if (fdSn == null || foundItemRepository.existsByAtcIdAndFdSn(atcId, fdSn)) {
			return false;
		}

		try {
			Optional<PoliceFoundItemDetailResponse> detail = includeDetail ? fetchDetail(atcId, fdSn) : Optional.empty();
			FoundItem foundItem = mapper.toFoundItem(response, detail.orElse(null));
			FoundItem savedItem = foundItemRepository.save(foundItem);
			if (detail.isPresent()) {
				imageService.saveImageIfPresent(savedItem, detail.get());
			} else {
				imageService.saveImageIfPresent(savedItem, response);
			}
			return true;
		} catch (DataIntegrityViolationException exception) {
			log.debug("이미 저장된 경찰청 습득물입니다. atcId={}, fdSn={}", atcId, fdSn, exception);
			return false;
		} catch (IllegalArgumentException exception) {
			log.warn("경찰청 습득물 응답을 저장하지 못했습니다. atcId={}, fdSn={}, reason={}", atcId, fdSn, exception.getMessage());
			return false;
		}
	}

	private Optional<PoliceFoundItemDetailResponse> fetchDetail(String atcId, Integer fdSn) {
		try {
			return client.fetchFoundItemDetail(atcId, fdSn);
		} catch (RuntimeException exception) {
			log.warn("경찰청 습득물 상세 조회에 실패했습니다. 목록 정보만 저장합니다. atcId={}, fdSn={}", atcId, fdSn, exception);
			return Optional.empty();
		}
	}
}
