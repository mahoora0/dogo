package com.example.dogo.service;

import com.example.dogo.entity.FoundItem;
import com.example.dogo.entity.ItemMatch;
import com.example.dogo.entity.LostItem;
import com.example.dogo.entity.User;
import com.example.dogo.repository.FoundItemImageRepository;
import com.example.dogo.repository.FoundItemRepository;
import com.example.dogo.repository.ItemMatchRepository;
import com.example.dogo.repository.LostItemImageRepository;
import com.example.dogo.repository.LostItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemMatchServiceTest {

	@Mock
	private ItemMatchRepository itemMatchRepository;

	@Mock
	private FoundItemRepository foundItemRepository;

	@Mock
	private LostItemRepository lostItemRepository;

	@Mock
	private FoundItemImageRepository foundItemImageRepository;

	@Mock
	private LostItemImageRepository lostItemImageRepository;

	private ItemMatchService itemMatchService;

	@BeforeEach
	void setUp() {
		MatchTextNormalizer normalizer = new MatchTextNormalizer();
		ItemMatchScorer scorer = new ItemMatchScorer(normalizer, new MatchTextTokenizer(normalizer));
		itemMatchService = new ItemMatchService(
				itemMatchRepository,
				foundItemRepository,
				lostItemRepository,
				foundItemImageRepository,
				lostItemImageRepository,
				scorer
		);
	}

	@Test
	void matchForLostItemUsesDedicatedCandidateQueryAndRequiresConcreteEvidence() {
		LocalDateTime lostAt = LocalDateTime.of(2026, 5, 10, 18, 0);
		LostItem lost = lostItem(1L, "검정색루에브르지갑", "지갑", lostAt, "서울", "강남역");
		FoundItem strong = foundItem(10L, "블랙 여성 반지갑", "지갑", lostAt.plusHours(2), "서울", "강남역", "블랙");
		FoundItem weak = foundItem(11L, "목걸이", "지갑", lostAt.plusHours(1), "부산", "서면로", "검정");

		when(foundItemRepository.findMatchCandidatesForLost(
				eq("지갑"),
				eq(lostAt.minusDays(3)),
				eq(lostAt.plusDays(60)),
				any(Pageable.class)
		)).thenReturn(List.of(strong, weak));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(1L, 10L)).thenReturn(false);

		itemMatchService.matchForLostItem(lost);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		verify(itemMatchRepository).save(captor.capture());
		assertThat(captor.getValue().getFoundItem().getFoundId()).isEqualTo(10L);
		assertThat(captor.getValue().getMatchScore()).isGreaterThanOrEqualTo(new BigDecimal("45.00"));
	}

	@Test
	void matchForFoundItemUsesDedicatedCandidateQueryWithTimeWindow() {
		LocalDateTime foundAt = LocalDateTime.of(2026, 5, 10, 20, 0);
		FoundItem found = foundItem(20L, "블랙 여성 반지갑", "지갑", foundAt, "서울", "강남역", "블랙");
		LostItem lost = lostItem(2L, "검정색루에브르지갑", "지갑", foundAt.minusHours(2), "서울", "강남역");

		when(lostItemRepository.findMatchCandidatesForFound(
				eq("지갑"),
				eq(foundAt.minusDays(60)),
				eq(foundAt.plusDays(3)),
				any(Pageable.class)
		)).thenReturn(List.of(lost));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(2L, 20L)).thenReturn(false);

		itemMatchService.matchForFoundItem(found);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		verify(itemMatchRepository).save(captor.capture());
		assertThat(captor.getValue().getLostItem().getLostId()).isEqualTo(2L);
		assertThat(captor.getValue().getFoundItem().getFoundId()).isEqualTo(20L);
	}

	private LostItem lostItem(Long id, String itemName, String category, LocalDateTime lostAt, String area, String place) {
		LostItem lostItem = new LostItem(
				new User("lost@dogo.local", "분실자", "010-1111-2222"),
				itemName,
				null,
					itemName,
					category,
					null,
					null,
					lostAt,
				area,
				place,
				"010-0000-0000"
		);
		ReflectionTestUtils.setField(lostItem, "lostId", id);
		return lostItem;
	}

	private FoundItem foundItem(Long id, String itemName, String category, LocalDateTime foundAt, String area, String place, String color) {
		FoundItem foundItem = new FoundItem(
				new User("found@dogo.local", "습득자", "010-3333-4444"),
				itemName,
				itemName,
				category,
				null,
				foundAt,
				area,
				place,
				"경찰서",
				color,
				null,
				"02-0000-0000"
		);
		ReflectionTestUtils.setField(foundItem, "foundId", id);
		return foundItem;
	}
}
