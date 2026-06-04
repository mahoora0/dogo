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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoliceFoundItemSyncServiceTest {

	private PoliceFoundItemClient client;
	private PoliceCommonCodeClient commonCodeClient;
	private FoundItemRepository foundItemRepository;
	private PoliceFoundItemImageService imageService;
	private ApplicationEventPublisher eventPublisher;
	private PoliceFoundItemSyncService syncService;

	@BeforeEach
	void setUp() {
		client = mock(PoliceFoundItemClient.class);
		commonCodeClient = mock(PoliceCommonCodeClient.class);
		foundItemRepository = mock(FoundItemRepository.class);
		imageService = mock(PoliceFoundItemImageService.class);
		eventPublisher = mock(ApplicationEventPublisher.class);
		when(commonCodeClient.fetchRegionCodes()).thenReturn(List.of(
				new PoliceRegionCode("LCA000", "서울특별시"),
				new PoliceRegionCode("LCA020", "서울특별시 용산구")
		));
		AtomicLong idSequence = new AtomicLong(100L);
		when(foundItemRepository.save(any(FoundItem.class))).thenAnswer(invocation -> {
			FoundItem saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "foundId", idSequence.getAndIncrement());
			return saved;
		});
		syncService = new PoliceFoundItemSyncService(
				client,
				commonCodeClient,
				new PoliceFoundItemMapper(),
				new PoliceStationAddressResolver(),
				foundItemRepository,
				imageService,
				eventPublisher,
				100,
				2,
				1,
				30
		);
	}

	@Test
	void incrementalSyncFetchesDetailAndStopsAfterDuplicatePages() {
		LocalDate startDate = LocalDate.of(2026, 4, 11);
		LocalDate endDate = LocalDate.of(2026, 5, 11);

		when(client.fetchFoundItems(startDate, endDate, 1, 100, "LCA000")).thenReturn(page(item("F202605110000001", "1")));
		when(client.fetchFoundItems(startDate, endDate, 2, 100, "LCA000")).thenReturn(page(item("F202605100000001", "1")));
		when(client.fetchFoundItems(startDate, endDate, 3, 100, "LCA000")).thenReturn(page(item("F202605090000001", "1")));
		when(client.fetchFoundItemDetail("F202605110000001", 1)).thenReturn(Optional.of(detail("F202605110000001", "1")));
		when(foundItemRepository.findByAtcIdAndFdSn("F202605100000001", 1)).thenReturn(Optional.of(existingFoundItem("F202605100000001", 1)));
		when(foundItemRepository.findByAtcIdAndFdSn("F202605090000001", 1)).thenReturn(Optional.of(existingFoundItem("F202605090000001", 1)));

		PoliceFoundItemSyncResult result = syncService.syncIncremental(startDate, endDate);

		assertThat(result.fetchedCount()).isEqualTo(3);
		assertThat(result.savedCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isEqualTo(2);
		assertThat(result.pageCount()).isEqualTo(3);
		verify(client, never()).fetchFoundItems(startDate, endDate, 4, 100, "LCA000");

		ArgumentCaptor<FoundItem> captor = ArgumentCaptor.forClass(FoundItem.class);
		verify(foundItemRepository).save(captor.capture());
		assertThat(captor.getValue().getSourceType()).isEqualTo("POLICE");
		assertThat(captor.getValue().getAtcId()).isEqualTo("F202605110000001");
		assertThat(captor.getValue().getFdSn()).isEqualTo(1);
		assertThat(captor.getValue().getFoundArea()).isEqualTo("서울특별시");
		assertThat(captor.getValue().getContent()).isEqualTo("특이사항 : 없음");
		assertThat(captor.getValue().getFoundPlace()).isEqualTo("기차");
		assertThat(captor.getValue().getContact()).isEqualTo("서울역(한국철도공사) / 02-3149-2531");

		verify(imageService).saveImageIfPresent(captor.getValue(), detail("F202605110000001", "1"));
		verify(eventPublisher).publishEvent((Object) argThat(event ->
				event instanceof FoundItemMatchRequestedEvent e && e.foundId().equals(100L)));
	}

	@Test
	void backfillSyncFetchesDetailUntilEmptyPage() {
		LocalDate startDate = LocalDate.of(2026, 4, 11);
		LocalDate endDate = LocalDate.of(2026, 5, 11);

		PoliceFoundItemResponse newItem = item("F202605110000001", "1");
		when(client.fetchFoundItems(startDate, endDate, 1, 100, "LCA000")).thenReturn(page(newItem));
		when(client.fetchFoundItems(startDate, endDate, 2, 100, "LCA000")).thenReturn(page());
		when(client.fetchFoundItemDetail("F202605110000001", 1)).thenReturn(Optional.of(detail("F202605110000001", "1")));
		when(foundItemRepository.existsByAtcIdAndFdSn("F202605110000001", 1)).thenReturn(false);

		PoliceFoundItemSyncResult result = syncService.syncBackfill(startDate, endDate);

		assertThat(result.fetchedCount()).isEqualTo(1);
		assertThat(result.savedCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isZero();
		assertThat(result.pageCount()).isEqualTo(2);
		verify(client).fetchFoundItemDetail("F202605110000001", 1);
		verify(imageService).saveImageIfPresent(any(FoundItem.class), any(PoliceFoundItemDetailResponse.class));
	}

	@Test
	void backfillContinuesWithNextTopLevelRegionAfterEmptyRegion() {
		LocalDate startDate = LocalDate.of(2026, 4, 11);
		LocalDate endDate = LocalDate.of(2026, 5, 11);

		when(commonCodeClient.fetchRegionCodes()).thenReturn(List.of(
				new PoliceRegionCode("LCA000", "서울특별시"),
				new PoliceRegionCode("LCA020", "서울특별시 용산구"),
				new PoliceRegionCode("LCE000", "광주광역시"),
				new PoliceRegionCode("LCI000", "경기도")
		));
		when(client.fetchFoundItems(startDate, endDate, 1, 100, "LCA000")).thenReturn(page(item("F202605110000001", "1")));
		when(client.fetchFoundItems(startDate, endDate, 2, 100, "LCA000")).thenReturn(page());
		when(client.fetchFoundItems(startDate, endDate, 1, 100, "LCE000")).thenReturn(page());
		when(client.fetchFoundItems(startDate, endDate, 1, 100, "LCI000")).thenReturn(page(item("F202605110000002", "1")));
		when(client.fetchFoundItems(startDate, endDate, 2, 100, "LCI000")).thenReturn(page());
		when(client.fetchFoundItemDetail("F202605110000001", 1)).thenReturn(Optional.of(detail("F202605110000001", "1")));
		when(client.fetchFoundItemDetail("F202605110000002", 1)).thenReturn(Optional.of(detail("F202605110000002", "1")));

		PoliceFoundItemSyncResult result = syncService.syncBackfill(startDate, endDate);

		assertThat(result.fetchedCount()).isEqualTo(2);
		assertThat(result.savedCount()).isEqualTo(2);
		assertThat(result.skippedCount()).isZero();
		assertThat(result.pageCount()).isEqualTo(5);
		verify(client, never()).fetchFoundItems(startDate, endDate, 1, 100, "LCA020");
		verify(client).fetchFoundItems(startDate, endDate, 1, 100, "LCI000");
	}

	@Test
	void incrementalSyncUpdatesExistingPoliceFoundItemDetail() {
		LocalDate startDate = LocalDate.of(2026, 4, 11);
		LocalDate endDate = LocalDate.of(2026, 5, 11);
		FoundItem existing = FoundItem.fromPolice("F202605110000001", 1, "old title", null,
				"old item", "old category", null, null,
				LocalDate.of(2026, 5, 11).atStartOfDay(), "old area", "old found place",
				"old keep place", null, null, null, "KEEPING");
		ReflectionTestUtils.setField(existing, "foundId", 77L);

		when(client.fetchFoundItems(startDate, endDate, 1, 100, "LCA000")).thenReturn(page(item("F202605110000001", "1")));
		when(client.fetchFoundItems(startDate, endDate, 2, 100, "LCA000")).thenReturn(page());
		when(foundItemRepository.findByAtcIdAndFdSn("F202605110000001", 1)).thenReturn(Optional.of(existing));
		when(client.fetchFoundItemDetail("F202605110000001", 1)).thenReturn(Optional.of(detail("F202605110000001", "1")));

		PoliceFoundItemSyncResult result = syncService.syncIncremental(startDate, endDate);

		assertThat(result.fetchedCount()).isEqualTo(1);
		assertThat(result.savedCount()).isZero();
		assertThat(result.skippedCount()).isEqualTo(1);
		assertThat(existing.getTitle()).isNotEqualTo("old title");
		assertThat(existing.getItemName()).isNotEqualTo("old item");
		assertThat(existing.getFoundArea()).isNotEqualTo("old area");
		assertThat(existing.getFoundPlace()).isNotEqualTo("old found place");
		assertThat(existing.getKeepPlace()).isNotEqualTo("old keep place");
		assertThat(existing.getContact()).contains("02-3149-2531");
		verify(foundItemRepository, never()).save(any(FoundItem.class));
		verify(imageService, never()).saveImageIfPresent(any(FoundItem.class), any(PoliceFoundItemDetailResponse.class));
	}

	@Test
	void publicSyncEntrypointsAreTransactionalForExistingItemUpdates() throws Exception {
		assertThat(PoliceFoundItemSyncService.class.getMethod("syncBackfillLastMonth").isAnnotationPresent(Transactional.class))
				.isTrue();
		assertThat(PoliceFoundItemSyncService.class.getMethod("syncIncrementalLastMonth").isAnnotationPresent(Transactional.class))
				.isTrue();
	}

	private PoliceFoundItemPage page(PoliceFoundItemResponse... items) {
		return new PoliceFoundItemPage("00", "NORMAL SERVICE", items.length, List.of(items));
	}

	private FoundItem existingFoundItem(String atcId, Integer fdSn) {
		FoundItem item = FoundItem.fromPolice(atcId, fdSn, "existing title", null,
				"existing item", "existing category", null, null,
				LocalDate.of(2026, 5, 11).atStartOfDay(), "existing area", "existing found place",
				"existing keep place", null, null, null, "KEEPING");
		ReflectionTestUtils.setField(item, "foundId", 200L);
		return item;
	}

	private PoliceFoundItemResponse item(String atcId, String fdSn) {
		return new PoliceFoundItemResponse(
				atcId,
				"브라운(갈)",
				"서울역(한국철도공사)",
				"https://example.com/found/bread.jpg",
				"빵 봉투",
				"빵 봉투(브라운(갈)색)을 습득하여 보관하고 있습니다",
				fdSn,
				"2026-05-11",
				"쇼핑백 > 쇼핑백"
		);
	}

	private PoliceFoundItemDetailResponse detail(String atcId, String fdSn) {
		return new PoliceFoundItemDetailResponse(
				atcId,
				"보관중",
				"서울역(한국철도공사)",
				"https://example.com/found/bread.jpg",
				"20",
				"기차",
				"빵 봉투",
				fdSn,
				"2026-05-11",
				"기관보관",
				"O0000001",
				"서울역(한국철도공사)",
				"쇼핑백 > 쇼핑백",
				"02-3149-2531",
				"특이사항 : 없음",
				"브라운(갈)"
		);
	}
}
