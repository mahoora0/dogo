package com.example.dogo.service;

import com.example.dogo.dto.MatchCandidateView;
import com.example.dogo.entity.FoundItem;
import com.example.dogo.entity.FoundItemImage;
import com.example.dogo.entity.ItemMatch;
import com.example.dogo.entity.LostItem;
import com.example.dogo.entity.LostItemImage;
import com.example.dogo.repository.FoundItemImageRepository;
import com.example.dogo.repository.FoundItemRepository;
import com.example.dogo.repository.ItemMatchRepository;
import com.example.dogo.repository.LostItemImageRepository;
import com.example.dogo.repository.LostItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ItemMatchService {

	private static final Logger log = LoggerFactory.getLogger(ItemMatchService.class);

	private static final int MAX_CANDIDATES = 10;
	private static final BigDecimal MIN_SCORE = new BigDecimal("20.00");
	private static final int DATE_RANGE_DAYS = 14;

	private static final BigDecimal CATEGORY_WEIGHT = new BigDecimal("30");
	private static final BigDecimal DATE_WEIGHT = new BigDecimal("25");
	private static final BigDecimal AREA_WEIGHT = new BigDecimal("20");
	private static final BigDecimal KEYWORD_WEIGHT = new BigDecimal("25");

	private final ItemMatchRepository itemMatchRepository;
	private final FoundItemRepository foundItemRepository;
	private final LostItemRepository lostItemRepository;
	private final FoundItemImageRepository foundItemImageRepository;
	private final LostItemImageRepository lostItemImageRepository;

	public ItemMatchService(
			ItemMatchRepository itemMatchRepository,
			FoundItemRepository foundItemRepository,
			LostItemRepository lostItemRepository,
			FoundItemImageRepository foundItemImageRepository,
			LostItemImageRepository lostItemImageRepository
	) {
		this.itemMatchRepository = itemMatchRepository;
		this.foundItemRepository = foundItemRepository;
		this.lostItemRepository = lostItemRepository;
		this.foundItemImageRepository = foundItemImageRepository;
		this.lostItemImageRepository = lostItemImageRepository;
	}

	@Transactional
	public void matchForLostItem(LostItem lostItem) {
		List<FoundItem> candidates = foundItemRepository.search(
				null,
				lostItem.getCategoryMain(),
				null,
				null,
				PageRequest.of(0, 200)
		).getContent();

		List<ItemMatch> matches = new ArrayList<>();
		for (FoundItem found : candidates) {
			if (found.isDeleted()) {
				continue;
			}
			BigDecimal score = calculateScore(lostItem, found);
			if (score.compareTo(MIN_SCORE) >= 0) {
				matches.add(new ItemMatch(lostItem, found, score));
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
	public void matchForFoundItem(FoundItem foundItem) {
		List<LostItem> candidates = lostItemRepository.search(
				null,
				foundItem.getCategoryMain(),
				null,
				null,
				PageRequest.of(0, 200)
		).getContent();

		List<ItemMatch> matches = new ArrayList<>();
		for (LostItem lost : candidates) {
			if (lost.isDeleted()) {
				continue;
			}
			BigDecimal score = calculateScore(lost, foundItem);
			if (score.compareTo(MIN_SCORE) >= 0) {
				matches.add(new ItemMatch(lost, foundItem, score));
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
							match.getMatchScore(),
							buildReasons(lost, found)
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
							match.getMatchScore(),
							buildReasons(lost, found)
					);
				})
				.toList();
	}

	BigDecimal calculateScore(LostItem lost, FoundItem found) {
		BigDecimal score = BigDecimal.ZERO;
		score = score.add(categoryScore(lost.getCategoryMain(), found.getCategoryMain()));
		score = score.add(dateScore(lost.getLostAt(), found.getFoundAt()));
		score = score.add(areaScore(lost.getLostArea(), lost.getLostPlace(), found.getFoundArea(), found.getFoundPlace()));
		score = score.add(keywordScore(lost, found));
		return score.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal categoryScore(String lostCategory, String foundCategory) {
		if (!StringUtils.hasText(lostCategory) || !StringUtils.hasText(foundCategory)) {
			return BigDecimal.ZERO;
		}
		if (lostCategory.trim().equals(foundCategory.trim())) {
			return CATEGORY_WEIGHT;
		}
		return BigDecimal.ZERO;
	}

	private BigDecimal dateScore(LocalDateTime lostAt, LocalDateTime foundAt) {
		if (lostAt == null || foundAt == null) {
			return BigDecimal.ZERO;
		}
		long daysDiff = Math.abs(Duration.between(lostAt, foundAt).toDays());
		if (daysDiff <= 1) {
			return DATE_WEIGHT;
		}
		if (daysDiff <= 3) {
			return DATE_WEIGHT.multiply(new BigDecimal("0.8"));
		}
		if (daysDiff <= 7) {
			return DATE_WEIGHT.multiply(new BigDecimal("0.5"));
		}
		if (daysDiff <= DATE_RANGE_DAYS) {
			return DATE_WEIGHT.multiply(new BigDecimal("0.2"));
		}
		return BigDecimal.ZERO;
	}

	private BigDecimal areaScore(String lostArea, String lostPlace, String foundArea, String foundPlace) {
		String lostText = combineText(lostArea, lostPlace);
		String foundText = combineText(foundArea, foundPlace);
		if (lostText.isEmpty() || foundText.isEmpty()) {
			return BigDecimal.ZERO;
		}

		Set<String> lostTokens = tokenize(lostText);
		Set<String> foundTokens = tokenize(foundText);
		if (lostTokens.isEmpty() || foundTokens.isEmpty()) {
			return BigDecimal.ZERO;
		}

		long commonCount = lostTokens.stream().filter(foundTokens::contains).count();
		if (commonCount == 0) {
			return BigDecimal.ZERO;
		}

		double ratio = (double) commonCount / Math.max(lostTokens.size(), foundTokens.size());
		return AREA_WEIGHT.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal keywordScore(LostItem lost, FoundItem found) {
		String lostText = combineText(lost.getItemName(), lost.getTitle(), lost.getContent());
		String foundText = combineText(found.getItemName(), found.getTitle(), found.getContent(),
				found.getColorName());
		if (lostText.isEmpty() || foundText.isEmpty()) {
			return BigDecimal.ZERO;
		}

		Set<String> lostTokens = tokenize(lostText);
		Set<String> foundTokens = tokenize(foundText);
		if (lostTokens.isEmpty() || foundTokens.isEmpty()) {
			return BigDecimal.ZERO;
		}

		long commonCount = lostTokens.stream().filter(foundTokens::contains).count();
		if (commonCount == 0) {
			return BigDecimal.ZERO;
		}

		double ratio = (double) commonCount / Math.max(lostTokens.size(), foundTokens.size());
		return KEYWORD_WEIGHT.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
	}

	private List<String> buildReasons(LostItem lost, FoundItem found) {
		List<String> reasons = new ArrayList<>();

		if (StringUtils.hasText(lost.getCategoryMain()) && StringUtils.hasText(found.getCategoryMain())
				&& lost.getCategoryMain().trim().equals(found.getCategoryMain().trim())) {
			reasons.add("카테고리 일치 (" + lost.getCategoryMain().trim() + ")");
		}

		if (lost.getLostAt() != null && found.getFoundAt() != null) {
			long daysDiff = Math.abs(Duration.between(lost.getLostAt(), found.getFoundAt()).toDays());
			if (daysDiff <= DATE_RANGE_DAYS) {
				reasons.add("날짜 " + daysDiff + "일 차이");
			}
		}

		String lostAreaText = combineText(lost.getLostArea(), lost.getLostPlace());
		String foundAreaText = combineText(found.getFoundArea(), found.getFoundPlace());
		Set<String> lostAreaTokens = tokenize(lostAreaText);
		Set<String> foundAreaTokens = tokenize(foundAreaText);
		List<String> commonArea = lostAreaTokens.stream().filter(foundAreaTokens::contains).toList();
		if (!commonArea.isEmpty()) {
			reasons.add("지역 키워드 일치 (" + String.join(", ", commonArea) + ")");
		}

		Set<String> lostKeywords = tokenize(combineText(lost.getItemName(), lost.getTitle()));
		Set<String> foundKeywords = tokenize(combineText(found.getItemName(), found.getTitle()));
		List<String> commonKeywords = lostKeywords.stream().filter(foundKeywords::contains).toList();
		if (!commonKeywords.isEmpty()) {
			reasons.add("물품명 키워드 일치 (" + String.join(", ", commonKeywords) + ")");
		}

		return reasons;
	}

	private String combineText(String... values) {
		StringBuilder sb = new StringBuilder();
		for (String value : values) {
			if (StringUtils.hasText(value)) {
				if (!sb.isEmpty()) {
					sb.append(" ");
				}
				sb.append(value.trim());
			}
		}
		return sb.toString();
	}

	private Set<String> tokenize(String text) {
		if (!StringUtils.hasText(text)) {
			return Set.of();
		}
		return Arrays.stream(text.toLowerCase().split("[\\s,./()>·]+"))
				.map(String::trim)
				.filter(token -> token.length() >= 2)
				.collect(Collectors.toSet());
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
