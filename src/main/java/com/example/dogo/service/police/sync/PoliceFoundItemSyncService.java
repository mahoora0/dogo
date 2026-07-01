package com.example.dogo.service.police.sync;

import com.example.dogo.dto.police.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.police.PoliceFoundItemPage;
import com.example.dogo.dto.police.PoliceFoundItemResponse;
import com.example.dogo.dto.police.PoliceFoundItemSyncResult;
import com.example.dogo.dto.police.PoliceRegionCode;
import com.example.dogo.service.police.client.PoliceCommonCodeClient;
import com.example.dogo.service.police.client.PoliceFoundItemClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
	private final PoliceFoundItemSaver saver;
	private final int numOfRows;
	private final int incrementalEmptyPageLimit;
	private final int backfillLookbackDays;
	private final int incrementalLookbackDays;

	public PoliceFoundItemSyncService(
			PoliceFoundItemClient client,
			PoliceCommonCodeClient commonCodeClient,
			PoliceFoundItemSaver saver,
			@Value("${police.found-item.num-of-rows:100}") int numOfRows,
			@Value("${police.found-item.incremental-empty-page-limit:2}") int incrementalEmptyPageLimit,
			@Value("${police.found-item.backfill-lookback-days:1}") int backfillLookbackDays,
			@Value("${police.found-item.incremental-lookback-days:30}") int incrementalLookbackDays
	) {
		this.client = client;
		this.commonCodeClient = commonCodeClient;
		this.saver = saver;
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

				// 상세 정보는 트랜잭션 밖에서 미리 조회 (HTTP 호출을 트랜잭션 안에서 하지 않음)
				Optional<PoliceFoundItemDetailResponse> detail = Optional.empty();
				if (includeDetail && response != null && StringUtils.hasText(response.atcId()) && StringUtils.hasText(response.fdSn())) {
					detail = fetchDetail(response.atcId().trim(), response.fdSn());
				}

				String regionName = regionCode == null ? null : regionCode.name();

				// 각 item을 독립 트랜잭션으로 저장 (saver 빈을 통해 프록시 적용)
				if (saver.saveIfNew(response, detail, regionName)) {
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

	private Optional<PoliceFoundItemDetailResponse> fetchDetail(String atcId, String fdSn) {
		try {
			Integer fdSnInt = parseOptionalFdSn(fdSn);
			if (fdSnInt == null) {
				return Optional.empty();
			}
			return client.fetchFoundItemDetail(atcId, fdSnInt);
		} catch (RuntimeException exception) {
			log.warn("경찰청 습득물 상세 조회에 실패했습니다. 목록 정보만 저장합니다. atcId={}", atcId, exception);
			return Optional.empty();
		}
	}

	private Integer parseOptionalFdSn(String fdSn) {
		if (!StringUtils.hasText(fdSn)) {
			return null;
		}
		try {
			return Integer.parseInt(fdSn.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
