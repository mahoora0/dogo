package com.example.dogo.service.police.sync;

import com.example.dogo.dto.police.PoliceLostItemDetailResponse;
import com.example.dogo.dto.police.PoliceLostItemPage;
import com.example.dogo.dto.police.PoliceLostItemResponse;
import com.example.dogo.dto.police.PoliceLostItemSyncResult;
import com.example.dogo.service.police.client.PoliceLostItemClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PoliceLostItemSyncService {

	private static final Logger log = LoggerFactory.getLogger(PoliceLostItemSyncService.class);

	private final PoliceLostItemClient client;
	private final PoliceLostItemSaver saver;
	private final int numOfRows;
	private final int incrementalEmptyPageLimit;
	private final int backfillLookbackDays;
	private final int incrementalLookbackDays;

	public PoliceLostItemSyncService(
			PoliceLostItemClient client,
			PoliceLostItemSaver saver,
			@Value("${police.lost-item.num-of-rows:100}") int numOfRows,
			@Value("${police.lost-item.incremental-empty-page-limit:2}") int incrementalEmptyPageLimit,
			@Value("${police.lost-item.backfill-lookback-days:1}") int backfillLookbackDays,
			@Value("${police.lost-item.incremental-lookback-days:30}") int incrementalLookbackDays
	) {
		this.client = client;
		this.saver = saver;
		this.numOfRows = numOfRows;
		this.incrementalEmptyPageLimit = incrementalEmptyPageLimit;
		this.backfillLookbackDays = Math.max(backfillLookbackDays, 1);
		this.incrementalLookbackDays = Math.max(incrementalLookbackDays, 1);
	}

	public PoliceLostItemSyncResult syncBackfillLastMonth() {
		LocalDate endDate = LocalDate.now();
		return syncBackfill(endDate.minusDays(backfillLookbackDays), endDate);
	}

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

				// 상세 정보는 트랜잭션 밖에서 미리 조회 (HTTP 호출을 트랜잭션 안에서 하지 않음)
				Optional<PoliceLostItemDetailResponse> detail = Optional.empty();
				if (includeDetail && response != null && org.springframework.util.StringUtils.hasText(response.atcId())) {
					detail = fetchDetail(response.atcId().trim());
				}

				// 각 item을 독립 트랜잭션으로 저장 (saver 빈을 통해 프록시 적용)
				if (saver.saveIfNew(response, includeDetail, detail)) {
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

	private Optional<PoliceLostItemDetailResponse> fetchDetail(String atcId) {
		try {
			return client.fetchLostItemDetail(atcId);
		} catch (RuntimeException exception) {
			log.warn("경찰청 분실물 상세 조회에 실패했습니다. 목록 정보만 저장합니다. atcId={}", atcId, exception);
			return Optional.empty();
		}
	}
}
