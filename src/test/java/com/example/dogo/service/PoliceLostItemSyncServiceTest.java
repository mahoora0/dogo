package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemPage;
import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.dto.PoliceLostItemResponse;
import com.example.dogo.dto.PoliceLostItemSyncResult;
import com.example.dogo.entity.LostItem;
import com.example.dogo.repository.LostItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoliceLostItemSyncServiceTest {

	private PoliceLostItemClient client;
	private LostItemRepository lostItemRepository;
	private PoliceLostItemImageService imageService;
	private PoliceLostItemSyncService syncService;

	@BeforeEach
	void setUp() {
		client = mock(PoliceLostItemClient.class);
		lostItemRepository = mock(LostItemRepository.class);
		imageService = mock(PoliceLostItemImageService.class);
		when(lostItemRepository.save(any(LostItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		syncService = new PoliceLostItemSyncService(
				client,
				new PoliceLostItemMapper(),
				lostItemRepository,
				imageService,
				100,
				2,
				1,
				30
		);
	}

	@Test
	void incrementalSyncStopsAfterTwoPagesWithoutNewItems() {
		LocalDate startDate = LocalDate.of(2026, 4, 11);
		LocalDate endDate = LocalDate.of(2026, 5, 11);

		when(client.fetchLostItems(startDate, endDate, 1, 100)).thenReturn(page(item("L202605090000001")));
		when(client.fetchLostItems(startDate, endDate, 2, 100)).thenReturn(page(item("L202605080000001")));
		when(client.fetchLostItems(startDate, endDate, 3, 100)).thenReturn(page(item("L202605070000001")));
		when(client.fetchLostItemDetail("L202605090000001")).thenReturn(Optional.of(detail(
				"L202605090000001",
				"https://example.com/lost/wallet.jpg"
		)));
		when(lostItemRepository.existsByAtcId("L202605090000001")).thenReturn(false);
		when(lostItemRepository.existsByAtcId("L202605080000001")).thenReturn(true);
		when(lostItemRepository.existsByAtcId("L202605070000001")).thenReturn(true);

		PoliceLostItemSyncResult result = syncService.syncIncremental(startDate, endDate);

		assertThat(result.fetchedCount()).isEqualTo(3);
		assertThat(result.savedCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isEqualTo(2);
		assertThat(result.pageCount()).isEqualTo(3);
		verify(client, never()).fetchLostItems(startDate, endDate, 4, 100);

		ArgumentCaptor<LostItem> captor = ArgumentCaptor.forClass(LostItem.class);
		verify(lostItemRepository).save(captor.capture());
		assertThat(captor.getValue().getSourceType()).isEqualTo("POLICE");
		assertThat(captor.getValue().getAtcId()).isEqualTo("L202605090000001");
		assertThat(captor.getValue().getContent()).isEqualTo("개인정보보호정책에 의해 정보가 제공되지 않습니다.");
		assertThat(captor.getValue().getLostArea()).isEqualTo("대전광역시");
		assertThat(captor.getValue().getContact()).isEqualTo("대전역지구대 / 042-271-0112");

		verify(imageService).saveImageIfPresent(captor.getValue(), detail(
				"L202605090000001",
				"https://example.com/lost/wallet.jpg"
		));
	}

	@Test
	void backfillSyncContinuesUntilEmptyPage() {
		LocalDate startDate = LocalDate.of(2026, 4, 11);
		LocalDate endDate = LocalDate.of(2026, 5, 11);

		when(client.fetchLostItems(startDate, endDate, 1, 100)).thenReturn(page(item("L202605090000001")));
		when(client.fetchLostItems(startDate, endDate, 2, 100)).thenReturn(page(item("L202605080000001")));
		when(client.fetchLostItems(startDate, endDate, 3, 100)).thenReturn(page());
		when(client.fetchLostItemDetail("L202605090000001")).thenReturn(Optional.of(detail(
				"L202605090000001",
				"https://minwon24.police.go.kr/images/sub/img02_no_img.gif"
		)));
		when(lostItemRepository.existsByAtcId("L202605090000001")).thenReturn(false);
		when(lostItemRepository.existsByAtcId("L202605080000001")).thenReturn(true);

		PoliceLostItemSyncResult result = syncService.syncBackfill(startDate, endDate);

		assertThat(result.fetchedCount()).isEqualTo(2);
		assertThat(result.savedCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isEqualTo(1);
		assertThat(result.pageCount()).isEqualTo(3);
		verify(client).fetchLostItemDetail("L202605090000001");
		verify(imageService).saveImageIfPresent(any(LostItem.class), any(PoliceLostItemDetailResponse.class));
	}

	@Test
	void configuredBackfillLookbackUsesOneDayByDefaultForDevelopment() {
		LocalDate today = LocalDate.now();
		when(client.fetchLostItems(today.minusDays(1), today, 1, 100)).thenReturn(page());

		syncService.syncBackfillLastMonth();

		verify(client).fetchLostItems(today.minusDays(1), today, 1, 100);
	}

	private PoliceLostItemPage page(PoliceLostItemResponse... items) {
		return new PoliceLostItemPage("00", "NORMAL SERVICE", items.length, List.of(items));
	}

	private PoliceLostItemResponse item(String atcId) {
		return new PoliceLostItemResponse(
				atcId,
				"검정 가방을 찾습니다",
				"노트북 가방",
				"2026-05-08",
				"강남역",
				"가방 > 기타가방"
		);
	}

	private PoliceLostItemDetailResponse detail(String atcId, String imageUrl) {
		return new PoliceLostItemDetailResponse(
				atcId,
				"루이까또즈 남성용 반지갑(블루(파랑)색)을 분실하였습니다.",
				"루이까또즈 남성용 반지갑",
				"2018-12-01",
				"21",
				"대흥동 택시 안",
				"지갑 > 남성용 지갑",
				"담당자 접수",
				"개인정보보호정책에 의해 정보가 제공되지 않습니다.",
				"대전광역시",
				"O0000673",
				"대전역지구대",
				"042-271-0112",
				"택시",
				imageUrl
		);
	}
}
