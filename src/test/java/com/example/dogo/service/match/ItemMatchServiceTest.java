package com.example.dogo.service.match;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.ItemMatch;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.item.FoundItemImageRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.ItemMatchRepository;
import com.example.dogo.repository.item.LostItemImageRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.service.match.semantic.SemanticMatchClient;
import com.example.dogo.service.match.semantic.SemanticMatchResponse;
import com.example.dogo.service.match.semantic.SemanticMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

	@Mock
	private SemanticMatchClient semanticMatchClient;

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
				scorer,
				semanticMatchClient
		);
	}

	// -----------------------------------------------------------------------
	// 기존 rule-only 동작 (semantic disabled → NoOp 대신 empty 응답 mock)
	// -----------------------------------------------------------------------

	@Test
	void matchForLostItem_semanticDisabled_savesRuleOnlyMatch() {
		LocalDateTime lostAt = LocalDateTime.of(2026, 5, 10, 18, 0);
		LostItem lost = lostItem(1L, "검정색루에브르지갑", "지갑", lostAt, "서울", "강남역");
		FoundItem strong = foundItem(10L, "블랙 여성 반지갑", "지갑", lostAt.plusHours(2), "서울", "강남역", "블랙");
		FoundItem weak = foundItem(11L, "목걸이", "지갑", lostAt.plusHours(1), "부산", "서면로", "검정");

		when(foundItemRepository.findMatchCandidatesForLost(
				eq("지갑"), eq(lostAt.minusDays(3)), eq(lostAt.plusDays(60))
		)).thenReturn(List.of(strong, weak));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(1L, 10L)).thenReturn(false);
		when(semanticMatchClient.score(any())).thenReturn(new SemanticMatchResponse(null, List.of()));

		itemMatchService.matchForLostItem(lost);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		verify(itemMatchRepository).save(captor.capture());
		ItemMatch saved = captor.getValue();

		assertThat(saved.getFoundItem().getFoundId()).isEqualTo(10L);
		assertThat(saved.getMatchScore()).isGreaterThanOrEqualTo(new BigDecimal("45.00"));
		assertThat(saved.getSemanticScore()).isNull();
		assertThat(saved.getMatchVersion()).isEqualTo("java-rule-v1");
		assertThat(saved.getModelName()).isNull();
	}

	@Test
	void matchForFoundItem_semanticDisabled_savesRuleOnlyMatch() {
		LocalDateTime foundAt = LocalDateTime.of(2026, 5, 10, 20, 0);
		FoundItem found = foundItem(20L, "블랙 여성 반지갑", "지갑", foundAt, "서울", "강남역", "블랙");
		LostItem lost = lostItem(2L, "검정색루에브르지갑", "지갑", foundAt.minusHours(2), "서울", "강남역");

		when(lostItemRepository.findMatchCandidatesForFound(
				eq("지갑"), eq(foundAt.minusDays(60)), eq(foundAt.plusDays(3))
		)).thenReturn(List.of(lost));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(2L, 20L)).thenReturn(false);
		when(semanticMatchClient.score(any())).thenReturn(new SemanticMatchResponse(null, List.of()));

		itemMatchService.matchForFoundItem(found);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		verify(itemMatchRepository).save(captor.capture());
		ItemMatch saved = captor.getValue();

		assertThat(saved.getLostItem().getLostId()).isEqualTo(2L);
		assertThat(saved.getSemanticScore()).isNull();
		assertThat(saved.getMatchVersion()).isEqualTo("java-rule-v1");
	}

	// -----------------------------------------------------------------------
	// semantic 성공: semanticScore, finalScore, matchVersion, modelName 저장
	// -----------------------------------------------------------------------

	@Test
	void matchForLostItem_semanticSuccess_savesFinalScoreAndVersion() {
		LocalDateTime lostAt = LocalDateTime.of(2026, 5, 10, 18, 0);
		LostItem lost = lostItem(1L, "검정색루에브르지갑", "지갑", lostAt, "서울", "강남역");
		FoundItem found = foundItem(10L, "블랙 여성 반지갑", "지갑", lostAt.plusHours(2), "서울", "강남역", "블랙");

		when(foundItemRepository.findMatchCandidatesForLost(any(), any(), any()))
				.thenReturn(List.of(found));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(1L, 10L)).thenReturn(false);

		SemanticMatchResult semResult = new SemanticMatchResult(10L, new BigDecimal("80.00"), List.of("물품명/제목 의미 유사"));
		when(semanticMatchClient.score(any()))
				.thenReturn(new SemanticMatchResponse("BM-K/KoSimCSE-roberta-multitask", List.of(semResult)));

		itemMatchService.matchForLostItem(lost);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		verify(itemMatchRepository).save(captor.capture());
		ItemMatch saved = captor.getValue();

		assertThat(saved.getSemanticScore()).isEqualByComparingTo(new BigDecimal("80.00"));
		assertThat(saved.getMatchVersion()).isEqualTo("java-rule-v1+kosimcse-v1");
		assertThat(saved.getModelName()).isEqualTo("BM-K/KoSimCSE-roberta-multitask");

		// finalScore = ruleScore * 0.7 + 80 * 0.3
		BigDecimal ruleScore = saved.getRuleScore();
		BigDecimal expectedFinal = ruleScore.multiply(new BigDecimal("0.7"))
				.add(new BigDecimal("80.00").multiply(new BigDecimal("0.3")));
		assertThat(saved.getMatchScore()).isEqualByComparingTo(expectedFinal.setScale(2, java.math.RoundingMode.HALF_UP));

		// Python reason이 matchReasons에 포함되어야 함
		assertThat(saved.getMatchReasonList()).anyMatch(r -> r.contains("의미 유사도"));
		assertThat(saved.getMatchReasonList()).contains("물품명/제목 의미 유사");
	}

	// -----------------------------------------------------------------------
	// semantic 전체 실패: rule-only fallback
	// -----------------------------------------------------------------------

	@Test
	void matchForLostItem_semanticException_fallsBackToRuleOnly() {
		LocalDateTime lostAt = LocalDateTime.of(2026, 5, 10, 18, 0);
		LostItem lost = lostItem(1L, "검정색루에브르지갑", "지갑", lostAt, "서울", "강남역");
		FoundItem found = foundItem(10L, "블랙 여성 반지갑", "지갑", lostAt.plusHours(2), "서울", "강남역", "블랙");

		when(foundItemRepository.findMatchCandidatesForLost(any(), any(), any()))
				.thenReturn(List.of(found));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(1L, 10L)).thenReturn(false);
		when(semanticMatchClient.score(any())).thenThrow(new RuntimeException("Python 서버 연결 실패"));

		itemMatchService.matchForLostItem(lost);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		verify(itemMatchRepository).save(captor.capture());
		ItemMatch saved = captor.getValue();

		assertThat(saved.getSemanticScore()).isNull();
		assertThat(saved.getMatchVersion()).isEqualTo("java-rule-v1");
		assertThat(saved.getModelName()).isNull();
		assertThat(saved.getMatchScore()).isEqualByComparingTo(saved.getRuleScore());
	}

	// -----------------------------------------------------------------------
	// semantic 결과 일부 누락: 해당 후보만 rule-only fallback
	// -----------------------------------------------------------------------

	@Test
	void matchForLostItem_semanticPartialResult_missingCandidateUsesRuleOnly() {
		LocalDateTime lostAt = LocalDateTime.of(2026, 5, 10, 18, 0);
		LostItem lost = lostItem(1L, "검정색루에브르지갑", "지갑", lostAt, "서울", "강남역");
		FoundItem found1 = foundItem(10L, "블랙 여성 반지갑", "지갑", lostAt.plusHours(1), "서울", "강남역", "블랙");
		FoundItem found2 = foundItem(11L, "검정 지갑", "지갑", lostAt.plusHours(2), "서울", "강남역", "검정");

		when(foundItemRepository.findMatchCandidatesForLost(any(), any(), any()))
				.thenReturn(List.of(found1, found2));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(eq(1L), any())).thenReturn(false);

		// found1(10)만 semantic 결과 있고 found2(11)는 누락
		SemanticMatchResult semResult = new SemanticMatchResult(10L, new BigDecimal("75.00"), List.of());
		when(semanticMatchClient.score(any()))
				.thenReturn(new SemanticMatchResponse("BM-K/KoSimCSE-roberta-multitask", List.of(semResult)));

		itemMatchService.matchForLostItem(lost);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		// found1과 found2 중 shouldStore 통과하는 것들이 저장됨
		// found1은 semantic 있음, found2는 없음
		List<ItemMatch> saved = captor.getAllValues();
		// 저장된 것들 중 foundId=10인 것은 semanticScore 있어야 하고,
		// foundId=11인 것은 semanticScore null이어야 함
		verify(itemMatchRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
		List<ItemMatch> allSaved = captor.getAllValues();

		allSaved.stream()
				.filter(m -> m.getFoundItem().getFoundId().equals(10L))
				.findFirst()
				.ifPresent(m -> assertThat(m.getSemanticScore()).isEqualByComparingTo(new BigDecimal("75.00")));

		allSaved.stream()
				.filter(m -> m.getFoundItem().getFoundId().equals(11L))
				.findFirst()
				.ifPresent(m -> {
					assertThat(m.getSemanticScore()).isNull();
					assertThat(m.getMatchVersion()).isEqualTo("java-rule-v1");
				});
	}

	@Test
	void matchForLostItem_capsFinalScoreWhenBroadAreaDiffersWithoutPlaceEvidence() {
		LocalDateTime lostAt = LocalDateTime.of(2026, 5, 13, 19, 0);
		LostItem lost = lostItem(1L, "검정색 반지갑", "지갑", lostAt, "서울", "강남역 2번 출구");
		FoundItem found = foundItem(10L, "검정색 반지갑", "지갑", lostAt.plusMinutes(90), "충남", "쌍용동 인도", "블랙");

		when(foundItemRepository.findMatchCandidatesForLost(any(), any(), any()))
				.thenReturn(List.of(found));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(1L, 10L)).thenReturn(false);
		when(semanticMatchClient.score(any()))
				.thenReturn(new SemanticMatchResponse("BM-K/KoSimCSE-roberta-multitask",
						List.of(new SemanticMatchResult(10L, new BigDecimal("100.00"), List.of("물품명/제목 의미 유사")))));

		itemMatchService.matchForLostItem(lost);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		verify(itemMatchRepository).save(captor.capture());
		ItemMatch saved = captor.getValue();

		assertThat(saved.getMatchScore()).isEqualByComparingTo(new BigDecimal("58.00"));
		assertThat(saved.getFinalScore()).isEqualByComparingTo(new BigDecimal("58.00"));
		assertThat(saved.getMatchReasonList()).contains("분실/습득 지역 차이가 커 점수 상한 적용 (58점)");
	}

	@Test
	void matchForLostItem_usesStrictCapWhenOnlyStationNameAndFoundPlaceHasMovementContext() {
		LocalDateTime lostAt = LocalDateTime.of(2026, 5, 13, 19, 0);
		LostItem lost = lostItem(1L, "검정색 반지갑", "지갑", lostAt, "서울", "강남역 2번 출구");
		FoundItem found = foundItem(10L, "검정색 반지갑", "지갑", lostAt.plusMinutes(90), "충남", "택시", "블랙");

		when(foundItemRepository.findMatchCandidatesForLost(any(), any(), any()))
				.thenReturn(List.of(found));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(1L, 10L)).thenReturn(false);
		when(semanticMatchClient.score(any()))
				.thenReturn(new SemanticMatchResponse("BM-K/KoSimCSE-roberta-multitask",
						List.of(new SemanticMatchResult(10L, new BigDecimal("100.00"), List.of("물품명/제목 의미 유사")))));

		itemMatchService.matchForLostItem(lost);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		verify(itemMatchRepository).save(captor.capture());
		ItemMatch saved = captor.getValue();

		assertThat(saved.getMatchScore()).isEqualByComparingTo(new BigDecimal("58.00"));
		assertThat(saved.getFinalScore()).isEqualByComparingTo(new BigDecimal("58.00"));
		assertThat(saved.getMatchReasonList()).contains("분실/습득 지역 차이가 커 점수 상한 적용 (58점)");
	}

	@Test
	void matchForLostItem_usesSofterCapWhenBothPlacesHaveMovementContext() {
		LocalDateTime lostAt = LocalDateTime.of(2026, 5, 13, 19, 0);
		LostItem lost = lostItem(1L, "검정색 반지갑", "지갑", lostAt, "서울", "강남역 택시 승강장");
		FoundItem found = foundItem(10L, "검정색 반지갑", "지갑", lostAt.plusMinutes(90), "충남", "택시", "블랙");

		when(foundItemRepository.findMatchCandidatesForLost(any(), any(), any()))
				.thenReturn(List.of(found));
		when(itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(1L, 10L)).thenReturn(false);
		when(semanticMatchClient.score(any()))
				.thenReturn(new SemanticMatchResponse("BM-K/KoSimCSE-roberta-multitask",
						List.of(new SemanticMatchResult(10L, new BigDecimal("100.00"), List.of("물품명/제목 의미 유사")))));

		itemMatchService.matchForLostItem(lost);

		ArgumentCaptor<ItemMatch> captor = ArgumentCaptor.forClass(ItemMatch.class);
		verify(itemMatchRepository).save(captor.capture());
		ItemMatch saved = captor.getValue();

		assertThat(saved.getMatchScore()).isEqualByComparingTo(new BigDecimal("62.00"));
		assertThat(saved.getFinalScore()).isEqualByComparingTo(new BigDecimal("62.00"));
		assertThat(saved.getMatchReasonList()).contains("분실/습득 지역 차이가 커 점수 상한 적용 (62점)");
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

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
