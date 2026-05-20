package com.example.dogo.service.police.sync;

import com.example.dogo.dto.police.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.police.PoliceFoundItemPage;
import com.example.dogo.dto.police.PoliceFoundItemResponse;
import com.example.dogo.dto.police.PoliceFoundItemSyncResult;
import com.example.dogo.dto.police.PoliceRegionCode;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.service.match.FoundItemMatchRequestedEvent;
import com.example.dogo.service.police.client.PoliceCommonCodeClient;
import com.example.dogo.service.police.client.PoliceFoundItemClient;
import com.example.dogo.service.police.mapper.PoliceFoundItemMapper;
import com.example.dogo.service.police.station.PoliceStationAddressResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PoliceFoundItemSyncService {

	private static final Logger log = LoggerFactory.getLogger(PoliceFoundItemSyncService.class);

	private final PoliceFoundItemClient client;
	private final PoliceCommonCodeClient commonCodeClient;
	private final PoliceFoundItemMapper mapper;
	private final PoliceStationAddressResolver stationAddressResolver;
	private final FoundItemRepository foundItemRepository;
	private final PoliceFoundItemImageService imageService;
	private final ApplicationEventPublisher eventPublisher;
	private final int numOfRows;
	private final int incrementalEmptyPageLimit;
	private final int backfillLookbackDays;
	private final int incrementalLookbackDays;

	public PoliceFoundItemSyncService(
			PoliceFoundItemClient client,
			PoliceCommonCodeClient commonCodeClient,
			PoliceFoundItemMapper mapper,
			PoliceStationAddressResolver stationAddressResolver,
			FoundItemRepository foundItemRepository,
			PoliceFoundItemImageService imageService,
			ApplicationEventPublisher eventPublisher,
			@Value("${police.found-item.num-of-rows:100}") int numOfRows,
			@Value("${police.found-item.incremental-empty-page-limit:2}") int incrementalEmptyPageLimit,
			@Value("${police.found-item.backfill-lookback-days:1}") int backfillLookbackDays,
			@Value("${police.found-item.incremental-lookback-days:30}") int incrementalLookbackDays
	) {
		this.client = client;
		this.commonCodeClient = commonCodeClient;
		this.mapper = mapper;
		this.stationAddressResolver = stationAddressResolver;
		this.foundItemRepository = foundItemRepository;
		this.imageService = imageService;
		this.eventPublisher = eventPublisher;
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
		return syncByRegions(startDate, endDate, false, true);
	}

	PoliceFoundItemSyncResult syncIncremental(LocalDate startDate, LocalDate endDate) {
		return syncByRegions(startDate, endDate, true, true);
	}

	private PoliceFoundItemSyncResult syncByRegions(
			LocalDate startDate,
			LocalDate endDate,
			boolean stopAfterDuplicatePages,
			boolean includeDetail
	) {
		List<PoliceRegionCode> regionCodes = fetchTopLevelRegionCodes();
		if (regionCodes.isEmpty()) {
			return sync(startDate, endDate, stopAfterDuplicatePages, includeDetail, null);
		}

		int fetchedCount = 0;
		int savedCount = 0;
		int skippedCount = 0;
		int pageCount = 0;
		for (PoliceRegionCode regionCode : regionCodes) {
			PoliceFoundItemSyncResult result = sync(startDate, endDate, stopAfterDuplicatePages, includeDetail, regionCode);
			fetchedCount += result.fetchedCount();
			savedCount += result.savedCount();
			skippedCount += result.skippedCount();
			pageCount += result.pageCount();
		}
		return new PoliceFoundItemSyncResult(fetchedCount, savedCount, skippedCount, pageCount);
	}

	private List<PoliceRegionCode> fetchTopLevelRegionCodes() {
		try {
			List<PoliceRegionCode> regionCodes = commonCodeClient.fetchRegionCodes();
			if (regionCodes.isEmpty()) {
				return List.of();
			}
			return regionCodes.stream()
					.filter(region -> StringUtils.hasText(region.code()))
					.filter(region -> region.code().endsWith("000"))
					.toList();
		} catch (RuntimeException exception) {
			log.warn("경찰청 지역 공통코드 조회에 실패했습니다. 지역 없이 습득물을 동기화합니다.", exception);
			return List.of();
		}
	}

	private PoliceFoundItemSyncResult sync(
			LocalDate startDate,
			LocalDate endDate,
			boolean stopAfterDuplicatePages,
			boolean includeDetail,
			PoliceRegionCode regionCode
	) {
		int pageNo = 1;
		int emptyNewPageCount = 0;
		int fetchedCount = 0;
		int savedCount = 0;
		int skippedCount = 0;
		int pageCount = 0;

		while (true) {
			PoliceFoundItemPage page = client.fetchFoundItems(
					startDate,
					endDate,
					pageNo,
					numOfRows,
					regionCode == null ? null : regionCode.code()
			);
			pageCount++;

			if (page.items().isEmpty()) {
				break;
			}

			int newCount = 0;
			for (PoliceFoundItemResponse response : page.items()) {
				fetchedCount++;
				if (saveIfNew(response, includeDetail, regionCode == null ? null : regionCode.name())) {
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

	private boolean saveIfNew(PoliceFoundItemResponse response, boolean includeDetail, String regionName) {
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
			String foundArea = detail
					.flatMap(detailResponse -> stationAddressResolver.resolveFoundArea(detailResponse, regionName))
					.orElse(regionName);
			FoundItem foundItem = mapper.toFoundItem(response, detail.orElse(null), foundArea);
			FoundItem savedItem = foundItemRepository.save(foundItem);
			if (detail.isPresent()) {
				imageService.saveImageIfPresent(savedItem, detail.get());
			} else {
				imageService.saveImageIfPresent(savedItem, response);
			}
			eventPublisher.publishEvent(new FoundItemMatchRequestedEvent(savedItem.getFoundId()));
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
