package com.example.dogo.service.match;

import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.FoundItemImage;
import com.example.dogo.entity.item.ItemMatch;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.item.LostItemImage;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.item.FoundItemImageRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.ItemMatchRepository;
import com.example.dogo.repository.item.LostItemImageRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.service.match.semantic.SemanticMatchClient;
import com.example.dogo.service.match.semantic.SemanticMatchItem;
import com.example.dogo.service.match.semantic.SemanticMatchRequest;
import com.example.dogo.service.match.semantic.SemanticMatchResponse;
import com.example.dogo.service.match.semantic.SemanticMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemMatchService {

	private static final Logger log = LoggerFactory.getLogger(ItemMatchService.class);

	private static final int MAX_CANDIDATES = 10;
	private static final int LOST_DATE_MARGIN_DAYS = 3;
	private static final int MAX_MATCH_DAYS = 60;
	private static final BigDecimal MIN_SCORE = new BigDecimal("45.00");
	private static final BigDecimal MIN_LOCATION_EVIDENCE_SCORE = new BigDecimal("6.00");
	private static final BigDecimal RULE_WEIGHT = new BigDecimal("0.7");
	private static final BigDecimal SEMANTIC_WEIGHT = new BigDecimal("0.3");
	private static final BigDecimal AREA_MISMATCH_CAP = new BigDecimal("58.00");
	private static final BigDecimal TRANSPORT_AREA_MISMATCH_CAP = new BigDecimal("62.00");
	private static final String RULE_MATCH_VERSION = "java-rule-v1";
	private static final String SEMANTIC_MATCH_VERSION = "java-rule-v1+kosimcse-v1";

	private final ItemMatchRepository itemMatchRepository;
	private final FoundItemRepository foundItemRepository;
	private final LostItemRepository lostItemRepository;
	private final FoundItemImageRepository foundItemImageRepository;
	private final LostItemImageRepository lostItemImageRepository;
	private final ItemMatchScorer itemMatchScorer;
	private final SemanticMatchClient semanticMatchClient;

	public ItemMatchService(
			ItemMatchRepository itemMatchRepository,
			FoundItemRepository foundItemRepository,
			LostItemRepository lostItemRepository,
			FoundItemImageRepository foundItemImageRepository,
			LostItemImageRepository lostItemImageRepository,
			ItemMatchScorer itemMatchScorer,
			SemanticMatchClient semanticMatchClient
	) {
		this.itemMatchRepository = itemMatchRepository;
		this.foundItemRepository = foundItemRepository;
		this.lostItemRepository = lostItemRepository;
		this.foundItemImageRepository = foundItemImageRepository;
		this.lostItemImageRepository = lostItemImageRepository;
		this.itemMatchScorer = itemMatchScorer;
		this.semanticMatchClient = semanticMatchClient;
	}

	@Transactional
	public void matchForLostItemId(Long lostId) {
		LostItem lostItem = lostItemRepository.findById(lostId)
				.orElseThrow(() -> new IllegalArgumentException("분실물을 찾을 수 없습니다. lostId=" + lostId));
		matchForLostItem(lostItem);
	}

	@Transactional
	public void matchForLostItem(LostItem lostItem) {
		clearMatchesForLostItem(lostItem.getLostId());

		List<FoundItem> candidates = foundItemRepository.findMatchCandidatesForLost(
				lostItem.getCategoryMain(),
				lostItem.getLostAt().minusDays(LOST_DATE_MARGIN_DAYS),
				lostItem.getLostAt().plusDays(MAX_MATCH_DAYS)
		);

		ItemMatchScorer.LostItemContext lostCtx = itemMatchScorer.precompute(lostItem);
		List<RuleCandidate> ruleEligible = new ArrayList<>();
		for (FoundItem found : candidates) {
			MatchScoreResult score = itemMatchScorer.score(lostCtx, found);
			if (score.eligible() && shouldStore(score)) {
				ruleEligible.add(new RuleCandidate(lostItem, found, score));
			}
		}

		if (ruleEligible.isEmpty()) {
			log.info("분실물 매칭 완료: lostId={}, 후보=0건", lostItem.getLostId());
			return;
		}

		SemanticFetchResult semantic = fetchSemanticScores(
				SemanticMatchItem.fromLost(lostItem),
				ruleEligible.stream().map(rc -> SemanticMatchItem.fromFound(rc.found())).toList()
		);

		List<ItemMatch> matches = ruleEligible.stream()
				.map(rc -> buildMatch(rc.lost(), rc.found(), rc.score(),
						semantic.results().get(rc.found().getFoundId()),
						semantic.modelName()))
				.collect(Collectors.toCollection(ArrayList::new));

		matches.sort((a, b) -> b.displayScore().compareTo(a.displayScore()));
		List<ItemMatch> topMatches = matches.stream().limit(MAX_CANDIDATES).toList();

		for (ItemMatch match : topMatches) {
			if (!itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(
					lostItem.getLostId(), match.getFoundItem().getFoundId())) {
				itemMatchRepository.save(match);
			}
		}

		log.info("분실물 매칭 완료: lostId={}, 후보={}건", lostItem.getLostId(), topMatches.size());
	}

	@Transactional
	public void clearMatchesForLostItem(Long lostId) {
		itemMatchRepository.deleteByLostItemLostId(lostId);
		itemMatchRepository.flush();
	}

	@Transactional
	public void matchForFoundItemId(Long foundId) {
		FoundItem foundItem = foundItemRepository.findById(foundId)
				.orElseThrow(() -> new IllegalArgumentException("습득물을 찾을 수 없습니다. foundId=" + foundId));
		matchForFoundItem(foundItem);
	}

	@Transactional
	public void matchForFoundItem(FoundItem foundItem) {
		clearMatchesForFoundItem(foundItem.getFoundId());

		List<LostItem> candidates = lostItemRepository.findMatchCandidatesForFound(
				foundItem.getCategoryMain(),
				foundItem.getFoundAt().minusDays(MAX_MATCH_DAYS),
				foundItem.getFoundAt().plusDays(LOST_DATE_MARGIN_DAYS)
		);

		ItemMatchScorer.FoundItemContext foundCtx = itemMatchScorer.precompute(foundItem);
		List<RuleCandidate> ruleEligible = new ArrayList<>();
		for (LostItem lost : candidates) {
			MatchScoreResult score = itemMatchScorer.score(lost, foundCtx);
			if (score.eligible() && shouldStore(score)) {
				ruleEligible.add(new RuleCandidate(lost, foundItem, score));
			}
		}

		if (ruleEligible.isEmpty()) {
			log.info("습득물 매칭 완료: foundId={}, 후보=0건", foundItem.getFoundId());
			return;
		}

		SemanticFetchResult semantic = fetchSemanticScores(
				SemanticMatchItem.fromFound(foundItem),
				ruleEligible.stream().map(rc -> SemanticMatchItem.fromLost(rc.lost())).toList()
		);

		List<ItemMatch> matches = ruleEligible.stream()
				.map(rc -> buildMatch(rc.lost(), rc.found(), rc.score(),
						semantic.results().get(rc.lost().getLostId()),
						semantic.modelName()))
				.collect(Collectors.toCollection(ArrayList::new));

		matches.sort((a, b) -> b.displayScore().compareTo(a.displayScore()));
		List<ItemMatch> topMatches = matches.stream().limit(MAX_CANDIDATES).toList();

		for (ItemMatch match : topMatches) {
			if (!itemMatchRepository.existsByLostItemLostIdAndFoundItemFoundId(
					match.getLostItem().getLostId(), foundItem.getFoundId())) {
				itemMatchRepository.save(match);
			}
		}

		log.info("습득물 매칭 완료: foundId={}, 후보={}건", foundItem.getFoundId(), topMatches.size());
	}

	@Transactional
	public void clearMatchesForFoundItem(Long foundId) {
		itemMatchRepository.deleteByFoundItemFoundId(foundId);
		itemMatchRepository.flush();
	}

	@Transactional
	public List<MatchCandidateView> getMatchesForLostItem(Long lostId) {
		List<ItemMatch> matches = itemMatchRepository.findByLostIdWithFoundItem(lostId);
		matches.forEach(ItemMatch::markAsRead);

		return matches.stream()
				.limit(MAX_CANDIDATES)
				.map(match -> {
					FoundItem found = match.getFoundItem();
					LostItem lost = match.getLostItem();
					String imageUrl = foundItemImageRepository
							.findFirstByFoundItemOrderBySortOrderAscImageIdAsc(found)
							.map(FoundItemImage::getImageUrl)
							.orElse(null);
					return new MatchCandidateView(
							found.getFoundId(),
							"found",
							found.getTitle(),
							found.getItemName(),
							found.getCategoryMain(),
							found.getFoundArea(),
							found.getFoundPlace(),
							found.getFoundAt(),
							found.getStatus(),
							foundStatusLabel(found.getStatus()),
							imageUrl,
							match.displayScore(),
							matchReasons(match, lost, found)
					);
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public long getUnreadMatchCount(User user) {
		if (user == null) {
			return 0;
		}
		long count = itemMatchRepository.countByLostItemUserUserNoAndMatchStatus(user.getUserNo(), "CANDIDATE");
		return Math.min(count, 3);
	}

	@Transactional(readOnly = true)
	public List<MatchCandidateView> getTopMatchesForNotification(User user) {
		if (user == null) {
			return List.of();
		}

		return itemMatchRepository.findTop3ByLostItemUserUserNoOrderByFinalScoreDescMatchIdDesc(user.getUserNo())
				.stream()
				.map(match -> {
					FoundItem found = match.getFoundItem();
					String imageUrl = foundItemImageRepository
							.findFirstByFoundItemOrderBySortOrderAscImageIdAsc(found)
							.map(FoundItemImage::getImageUrl)
							.orElse(null);
					return new MatchCandidateView(
							found.getFoundId(),
							"found",
							found.getTitle(),
							found.getItemName(),
							found.getCategoryMain(),
							found.getFoundArea(),
							found.getFoundPlace(),
							found.getFoundAt(),
							found.getStatus(),
							foundStatusLabel(found.getStatus()),
							imageUrl,
							match.displayScore(),
							List.of()
					);
				})
				.toList();
	}

	@Transactional
	public void markAllAsRead(User user) {
		if (user != null) {
			itemMatchRepository.markAllAsReadByUserNo(user.getUserNo());
		}
	}

	@Transactional(readOnly = true)
	public List<MatchCandidateView> getMatchesForFoundItem(Long foundId) {
		return itemMatchRepository.findByFoundIdWithLostItem(foundId).stream()
				.limit(MAX_CANDIDATES)
				.map(match -> {
					LostItem lost = match.getLostItem();
					FoundItem found = match.getFoundItem();
					String imageUrl = lostItemImageRepository
							.findFirstByLostItemOrderBySortOrderAscImageIdAsc(lost)
							.map(LostItemImage::getImageUrl)
							.orElse(null);
					return new MatchCandidateView(
							lost.getLostId(),
							"lost",
							lost.getTitle(),
							lost.getItemName(),
							lost.getCategoryMain(),
							lost.getLostArea(),
							lost.getLostPlace(),
							lost.getLostAt(),
							lost.getStatus(),
							lostStatusLabel(lost.getStatus()),
							imageUrl,
							match.displayScore(),
							matchReasons(match, lost, found)
					);
				})
				.toList();
	}

	private SemanticFetchResult fetchSemanticScores(SemanticMatchItem query, List<SemanticMatchItem> candidates) {
		if (candidates.isEmpty()) {
			return SemanticFetchResult.empty();
		}
		try {
			SemanticMatchResponse response = semanticMatchClient.score(new SemanticMatchRequest(query, candidates));
			List<SemanticMatchResult> results = response.results() != null ? response.results() : List.of();
			Map<Long, SemanticMatchResult> resultMap = results.stream()
					.collect(Collectors.toMap(SemanticMatchResult::candidateId, r -> r));
			return new SemanticFetchResult(response.model(), resultMap);
		} catch (Exception e) {
			log.warn("시맨틱 매칭 클라이언트 호출 실패, rule-only 폴백 사용: {}", e.getMessage());
			return SemanticFetchResult.empty();
		}
	}

	private ItemMatch buildMatch(LostItem lost, FoundItem found, MatchScoreResult ruleScore,
			SemanticMatchResult sem, String semanticModelName) {
		BigDecimal semanticScore = sem != null ? sem.semanticScore() : null;
		BigDecimal finalScore = computeFinalScore(ruleScore.totalScore(), semanticScore);

		List<String> reasons = new ArrayList<>(ruleScore.reasons());
		String matchVersion = RULE_MATCH_VERSION;
		String modelName = null;

		if (sem != null && semanticScore != null) {
			reasons.add("의미 유사도 " + sem.semanticScore().setScale(2, RoundingMode.HALF_UP) + "점");
			if (sem.reasons() != null) {
				reasons.addAll(sem.reasons());
			}
			matchVersion = SEMANTIC_MATCH_VERSION;
			modelName = semanticModelName;
		}
		finalScore = applyLocationCap(lost, found, finalScore, reasons);

		return new ItemMatch(lost, found, ruleScore.totalScore(), semanticScore, finalScore,
				reasons, matchVersion, modelName);
	}

	private BigDecimal computeFinalScore(BigDecimal ruleScore, BigDecimal semanticScore) {
		if (semanticScore == null) {
			return ruleScore;
		}
		return ruleScore.multiply(RULE_WEIGHT)
				.add(semanticScore.multiply(SEMANTIC_WEIGHT))
				.setScale(2, RoundingMode.HALF_UP)
				.min(new BigDecimal("100.00"));
	}

	private BigDecimal applyLocationCap(LostItem lost, FoundItem found, BigDecimal finalScore, List<String> reasons) {
		BigDecimal cap = locationCap(lost, found);
		if (cap == null || finalScore.compareTo(cap) <= 0) {
			return finalScore;
		}
		reasons.add("분실/습득 지역 차이가 커 점수 상한 적용 (" + cap.setScale(0, RoundingMode.HALF_UP) + "점)");
		return cap;
	}

	private BigDecimal locationCap(LostItem lost, FoundItem found) {
		String lostBroadArea = broadArea(lost.getLostArea());
		String foundBroadArea = broadArea(found.getFoundArea());
		if (lostBroadArea == null || foundBroadArea == null || lostBroadArea.equals(foundBroadArea)) {
			return null;
		}
		if (hasCommonPlaceToken(lost, found)) {
			return null;
		}
		if (hasMovementContext(lost.getLostPlace()) && hasMovementContext(found.getFoundPlace(), found.getKeepPlace())) {
			return TRANSPORT_AREA_MISMATCH_CAP;
		}
		return AREA_MISMATCH_CAP;
	}

	private String broadArea(String area) {
		if (area == null || area.isBlank()) {
			return null;
		}
		String first = area.replace(",", " ").trim().split("\\s+")[0];
		return switch (first) {
			case "서울특별시" -> "서울";
			case "부산광역시" -> "부산";
			case "대구광역시" -> "대구";
			case "인천광역시" -> "인천";
			case "광주광역시" -> "광주";
			case "대전광역시" -> "대전";
			case "울산광역시" -> "울산";
			case "세종특별자치시" -> "세종";
			case "경기도" -> "경기";
			case "강원특별자치도", "강원도" -> "강원";
			case "충청북도" -> "충북";
			case "충청남도" -> "충남";
			case "전북특별자치도", "전라북도" -> "전북";
			case "전라남도" -> "전남";
			case "경상북도" -> "경북";
			case "경상남도" -> "경남";
			case "제주특별자치도" -> "제주";
			default -> first;
		};
	}

	private boolean hasCommonPlaceToken(LostItem lost, FoundItem found) {
		List<String> lostTokens = placeTokens(lost.getLostPlace());
		List<String> foundTokens = placeTokens(found.getFoundPlace(), found.getKeepPlace());
		return lostTokens.stream().anyMatch(foundTokens::contains);
	}

	private List<String> placeTokens(String... values) {
		String joined = String.join(" ", java.util.Arrays.stream(values)
				.filter(value -> value != null && !value.isBlank())
				.toList());
		if (joined.isBlank()) {
			return List.of();
		}
		return java.util.Arrays.stream(joined.toLowerCase()
						.replaceAll("[^가-힣a-z0-9\\s]", " ")
						.split("\\s+"))
				.map(String::trim)
				.filter(token -> token.length() >= 2)
				.filter(token -> !token.matches("\\d+"))
				.filter(token -> !isGenericPlaceToken(token))
				.distinct()
				.toList();
	}

	private boolean isGenericPlaceToken(String token) {
		return token.equals("택시")
				|| token.equals("버스")
				|| token.equals("지하철")
				|| token.equals("전철")
				|| token.equals("기차")
				|| token.equals("ktx")
				|| token.equals("열차")
				|| token.equals("공항")
				|| token.equals("비행기")
				|| token.equals("터미널")
				|| token.equals("정류장")
				|| token.equals("승강장")
				|| token.equals("인근")
				|| token.equals("근처");
	}

	private boolean hasMovementContext(String... values) {
		String text = String.join(" ", java.util.Arrays.stream(values)
				.filter(value -> value != null && !value.isBlank())
				.toList()).toLowerCase();
		return text.contains("택시")
				|| text.contains("버스")
				|| text.contains("지하철")
				|| text.contains("전철")
				|| text.contains("기차")
				|| text.contains("ktx")
				|| text.contains("열차")
				|| text.contains("공항")
				|| text.contains("비행기")
				|| text.contains("터미널")
				|| text.contains("정류장");
	}

	private List<String> matchReasons(ItemMatch match, LostItem lostItem, FoundItem foundItem) {
		List<String> storedReasons = match.getMatchReasonList();
		if (!storedReasons.isEmpty()) {
			return storedReasons;
		}
		return itemMatchScorer.score(lostItem, foundItem).reasons();
	}

	private boolean shouldStore(MatchScoreResult score) {
		if (score.totalScore().compareTo(MIN_SCORE) < 0) {
			return false;
		}
		return score.keywordScore().compareTo(BigDecimal.ZERO) > 0
				|| score.detailScore().compareTo(BigDecimal.ZERO) > 0
				|| score.locationScore().compareTo(MIN_LOCATION_EVIDENCE_SCORE) >= 0;
	}

	private String foundStatusLabel(String status) {
		return switch (status) {
			case "MATCHING" -> "매칭중";
			case "RETURNED" -> "수령완료";
			default -> "보관중";
		};
	}

	private String lostStatusLabel(String status) {
		return switch (status) {
			case "MATCHING" -> "매칭중";
			case "FOUND" -> "회수완료";
			default -> "대기중";
		};
	}

	@Transactional
	public void matchForUserLostItems(User user) {
		if (user == null) {
			return;
		}

		List<LostItem> activeLostItems = lostItemRepository.findByUserAndDeletedFalseAndStatusIn(
				user, List.of("WAITING", "MATCHING"));

		for (LostItem lostItem : activeLostItems) {
			try {
				matchForLostItem(lostItem);
			} catch (Exception e) {
				log.error("Failed to refresh matches for user lost item. lostItemId={}",
						lostItem.getLostId(), e);
			}
		}
	}

	private record RuleCandidate(LostItem lost, FoundItem found, MatchScoreResult score) {
	}

	private record SemanticFetchResult(String modelName, Map<Long, SemanticMatchResult> results) {

		static SemanticFetchResult empty() {
			return new SemanticFetchResult(null, Map.of());
		}
	}
}
