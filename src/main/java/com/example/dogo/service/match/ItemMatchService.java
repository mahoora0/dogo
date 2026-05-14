package com.example.dogo.service.match;

import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.FoundItemImage;
import com.example.dogo.entity.item.ItemMatch;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.item.LostItemImage;
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
import org.springframework.data.domain.PageRequest;
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
	private static final int MATCH_SCAN_LIMIT = 500;
	private static final int LOST_DATE_MARGIN_DAYS = 3;
	private static final int MAX_MATCH_DAYS = 60;
	private static final BigDecimal MIN_SCORE = new BigDecimal("45.00");
	private static final BigDecimal MIN_LOCATION_EVIDENCE_SCORE = new BigDecimal("6.00");
	private static final BigDecimal RULE_WEIGHT = new BigDecimal("0.7");
	private static final BigDecimal SEMANTIC_WEIGHT = new BigDecimal("0.3");
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
		List<FoundItem> candidates = foundItemRepository.findMatchCandidatesForLost(
				lostItem.getCategoryMain(),
				lostItem.getLostAt().minusDays(LOST_DATE_MARGIN_DAYS),
				lostItem.getLostAt().plusDays(MAX_MATCH_DAYS),
				PageRequest.of(0, MATCH_SCAN_LIMIT)
		);

		List<RuleCandidate> ruleEligible = new ArrayList<>();
		for (FoundItem found : candidates) {
			MatchScoreResult score = itemMatchScorer.score(lostItem, found);
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
	public void matchForFoundItemId(Long foundId) {
		FoundItem foundItem = foundItemRepository.findById(foundId)
				.orElseThrow(() -> new IllegalArgumentException("습득물을 찾을 수 없습니다. foundId=" + foundId));
		matchForFoundItem(foundItem);
	}

	@Transactional
	public void matchForFoundItem(FoundItem foundItem) {
		List<LostItem> candidates = lostItemRepository.findMatchCandidatesForFound(
				foundItem.getCategoryMain(),
				foundItem.getFoundAt().minusDays(MAX_MATCH_DAYS),
				foundItem.getFoundAt().plusDays(LOST_DATE_MARGIN_DAYS),
				PageRequest.of(0, MATCH_SCAN_LIMIT)
		);

		List<RuleCandidate> ruleEligible = new ArrayList<>();
		for (LostItem lost : candidates) {
			MatchScoreResult score = itemMatchScorer.score(lost, foundItem);
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

	@Transactional(readOnly = true)
	public List<MatchCandidateView> getMatchesForLostItem(Long lostId) {
		return itemMatchRepository.findByLostIdWithFoundItem(lostId).stream()
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

	private record RuleCandidate(LostItem lost, FoundItem found, MatchScoreResult score) {
	}

	private record SemanticFetchResult(String modelName, Map<Long, SemanticMatchResult> results) {

		static SemanticFetchResult empty() {
			return new SemanticFetchResult(null, Map.of());
		}
	}
}
