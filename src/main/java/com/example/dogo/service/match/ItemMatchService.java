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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ItemMatchService {

	private static final Logger log = LoggerFactory.getLogger(ItemMatchService.class);

	private static final int MAX_CANDIDATES = 10;
	private static final int MATCH_SCAN_LIMIT = 500;
	private static final int LOST_DATE_MARGIN_DAYS = 3;
	private static final int MAX_MATCH_DAYS = 60;
	private static final BigDecimal MIN_SCORE = new BigDecimal("45.00");
	private static final BigDecimal MIN_LOCATION_EVIDENCE_SCORE = new BigDecimal("6.00");
	private static final String RULE_MATCH_VERSION = "java-rule-v1";

	private final ItemMatchRepository itemMatchRepository;
	private final FoundItemRepository foundItemRepository;
	private final LostItemRepository lostItemRepository;
	private final FoundItemImageRepository foundItemImageRepository;
	private final LostItemImageRepository lostItemImageRepository;
	private final ItemMatchScorer itemMatchScorer;

	public ItemMatchService(
			ItemMatchRepository itemMatchRepository,
			FoundItemRepository foundItemRepository,
			LostItemRepository lostItemRepository,
			FoundItemImageRepository foundItemImageRepository,
			LostItemImageRepository lostItemImageRepository,
			ItemMatchScorer itemMatchScorer
	) {
		this.itemMatchRepository = itemMatchRepository;
		this.foundItemRepository = foundItemRepository;
		this.lostItemRepository = lostItemRepository;
		this.foundItemImageRepository = foundItemImageRepository;
		this.lostItemImageRepository = lostItemImageRepository;
		this.itemMatchScorer = itemMatchScorer;
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

		List<ItemMatch> matches = new ArrayList<>();
		for (FoundItem found : candidates) {
			MatchScoreResult score = itemMatchScorer.score(lostItem, found);
			if (!score.eligible()) {
				continue;
			}
			if (shouldStore(score)) {
				matches.add(toRuleOnlyMatch(lostItem, found, score));
			}
		}

		matches.sort((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()));
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

		List<ItemMatch> matches = new ArrayList<>();
		for (LostItem lost : candidates) {
			MatchScoreResult score = itemMatchScorer.score(lost, foundItem);
			if (!score.eligible()) {
				continue;
			}
			if (shouldStore(score)) {
				matches.add(toRuleOnlyMatch(lost, foundItem, score));
			}
		}

		matches.sort((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()));
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

	private ItemMatch toRuleOnlyMatch(LostItem lostItem, FoundItem foundItem, MatchScoreResult score) {
		return new ItemMatch(
				lostItem,
				foundItem,
				score.totalScore(),
				null,
				score.totalScore(),
				score.reasons(),
				RULE_MATCH_VERSION,
				null
		);
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
}
