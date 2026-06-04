package com.example.dogo.service.police.sync;

import com.example.dogo.dto.police.PoliceLostItemPage;
import com.example.dogo.dto.police.PoliceLostItemDetailResponse;
import com.example.dogo.dto.police.PoliceLostItemResponse;
import com.example.dogo.dto.police.PoliceLostItemSyncResult;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.service.match.LostItemMatchRequestedEvent;
import com.example.dogo.service.police.client.PoliceLostItemClient;
import com.example.dogo.service.police.mapper.PoliceLostItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PoliceLostItemSyncService {

	private static final Logger log = LoggerFactory.getLogger(PoliceLostItemSyncService.class);

	private final PoliceLostItemClient client;
	private final PoliceLostItemMapper mapper;
	private final LostItemRepository lostItemRepository;
	private final PoliceLostItemImageService imageService;
	private final ApplicationEventPublisher eventPublisher;
	private final int numOfRows;
	private final int incrementalEmptyPageLimit;
	private final int backfillLookbackDays;
	private final int incrementalLookbackDays;

	public PoliceLostItemSyncService(
			PoliceLostItemClient client,
			PoliceLostItemMapper mapper,
			LostItemRepository lostItemRepository,
			PoliceLostItemImageService imageService,
			ApplicationEventPublisher eventPublisher,
			@Value("${police.lost-item.num-of-rows:100}") int numOfRows,
			@Value("${police.lost-item.incremental-empty-page-limit:2}") int incrementalEmptyPageLimit,
			@Value("${police.lost-item.backfill-lookback-days:1}") int backfillLookbackDays,
			@Value("${police.lost-item.incremental-lookback-days:30}") int incrementalLookbackDays
	) {
		this.client = client;
		this.mapper = mapper;
		this.lostItemRepository = lostItemRepository;
		this.imageService = imageService;
		this.eventPublisher = eventPublisher;
		this.numOfRows = numOfRows;
		this.incrementalEmptyPageLimit = incrementalEmptyPageLimit;
		this.backfillLookbackDays = Math.max(backfillLookbackDays, 1);
		this.incrementalLookbackDays = Math.max(incrementalLookbackDays, 1);
	}

	@Transactional
	public PoliceLostItemSyncResult syncBackfillLastMonth() {
		LocalDate endDate = LocalDate.now();
		return syncBackfill(endDate.minusDays(backfillLookbackDays), endDate);
	}

	@Transactional
	public PoliceLostItemSyncResult syncIncrementalLastMonth() {
		LocalDate endDate = LocalDate.now();
		return syncIncremental(endDate.minusDays(incrementalLookbackDays), endDate);
	}

	PoliceLostItemSyncResult syncBackfill(LocalDate startDate, LocalDate endDate) {
		return sync(startDate, endDate, false, true);
	}

	PoliceLostItemSyncResult syncIncremental(LocalDate startDate, LocalDate endDate) {
		return sync(startDate, endDate, true, true);
	}

	private PoliceLostItemSyncResult sync(
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
			PoliceLostItemPage page = client.fetchLostItems(startDate, endDate, pageNo, numOfRows);
			pageCount++;

			if (page.items().isEmpty()) {
				break;
			}

			int newCount = 0;
			for (PoliceLostItemResponse response : page.items()) {
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

		return new PoliceLostItemSyncResult(fetchedCount, savedCount, skippedCount, pageCount);
	}

	private boolean saveIfNew(PoliceLostItemResponse response, boolean includeDetail) {
		if (response == null || !StringUtils.hasText(response.atcId())) {
			return false;
		}

		String atcId = response.atcId().trim();
		Optional<LostItem> existingItem = lostItemRepository.findByAtcId(atcId);
		if (existingItem.isPresent()) {
			updateExisting(existingItem.get(), response, includeDetail);
			return false;
		}

		try {
			Optional<PoliceLostItemDetailResponse> detail = includeDetail ? fetchDetail(atcId) : Optional.empty();
			LostItem lostItem = mapper.toLostItem(response, detail.orElse(null));
			LostItem savedItem = lostItemRepository.save(lostItem);
			detail.ifPresent(detailResponse -> imageService.saveImageIfPresent(savedItem, detailResponse));
			eventPublisher.publishEvent(new LostItemMatchRequestedEvent(savedItem.getLostId()));
			return true;
		} catch (DataIntegrityViolationException exception) {
			log.debug("이미 저장된 경찰청 분실물입니다. atcId={}", atcId, exception);
			return false;
		} catch (IllegalArgumentException exception) {
			log.warn("경찰청 분실물 응답을 저장하지 못했습니다. atcId={}, reason={}", atcId, exception.getMessage());
			return false;
		}
	}

	private void updateExisting(LostItem existingItem, PoliceLostItemResponse response, boolean includeDetail) {
		Optional<PoliceLostItemDetailResponse> detail = includeDetail ? fetchDetail(existingItem.getAtcId()) : Optional.empty();
		LostItem mappedItem = mapper.toLostItem(response, detail.orElse(null));
		existingItem.updatePoliceDetail(
				mappedItem.getTitle(),
				mappedItem.getContent(),
				mappedItem.getItemName(),
				mappedItem.getCategoryMain(),
				mappedItem.getCategorySub(),
				mappedItem.getColorName(),
				mappedItem.getLostAt(),
				mappedItem.getLostArea(),
				mappedItem.getLostPlace(),
				mappedItem.getContact()
		);
	}

	private Optional<PoliceLostItemDetailResponse> fetchDetail(String atcId) {
		try {
			return client.fetchLostItemDetail(atcId);
		} catch (RuntimeException exception) {
			log.warn("경찰청 분실물 상세 조회에 실패했습니다. 목록 정보만 저장합니다. atcId={}", atcId, exception);
			return Optional.empty();
		}
	}
}
