package com.example.dogo.service.match;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ItemMatchScorer {

	private static final int LOST_DATE_MARGIN_DAYS = 3;
	private static final int MAX_MATCH_DAYS = 60;

	private static final BigDecimal CATEGORY_MAIN_AND_SUB_SCORE = new BigDecimal("15");
	private static final BigDecimal CATEGORY_MAIN_SCORE = new BigDecimal("10");
	private static final BigDecimal CATEGORY_MISSING_SCORE = new BigDecimal("3");
	private static final BigDecimal LOCATION_WEIGHT = new BigDecimal("20");
	private static final BigDecimal KEYWORD_WEIGHT = new BigDecimal("25");
	private static final BigDecimal DETAIL_WEIGHT = new BigDecimal("5");
	private static final BigDecimal COLOR_MATCH_SCORE = new BigDecimal("10");
	private static final BigDecimal COLOR_PARTIAL_SCORE = new BigDecimal("2");
	static final BigDecimal TRANSPORT_LOCATION_FLOOR_SCORE = new BigDecimal("6");
	private static final Pattern MODEL_TOKEN_PATTERN = Pattern.compile("[a-z]+\\d+[a-z0-9]*|\\d+[a-z]+[a-z0-9]*|\\d{2,}");

	private final MatchTextNormalizer normalizer;
	private final MatchTextTokenizer tokenizer;
	private final MatchDictionary dictionary;

	public ItemMatchScorer(MatchTextNormalizer normalizer, MatchTextTokenizer tokenizer, MatchDictionary dictionary) {
		this.normalizer = normalizer;
		this.tokenizer = tokenizer;
		this.dictionary = dictionary;
	}

	// ─── Pre-computed context records ────────────────────────────────────────

	public record LostItemContext(
			LostItem item,
			Set<String> locationTokens,
			Set<String> keywordTokens,
			Set<String> detailTokens,
			Optional<String> color
	) {}

	public record FoundItemContext(
			FoundItem item,
			Set<String> locationTokens,
			Set<String> keywordTokens,
			Set<String> detailTokens,
			Optional<String> color
	) {}

	public LostItemContext precompute(LostItem lost) {
		return new LostItemContext(
				lost,
				tokenizer.tokenize(lost.getLostArea(), lost.getLostPlace()),
				tokenizer.tokenize(lost.getItemName()),
				tokenizer.tokenize(lost.getContent()),
				normalizer.extractColor(lost.getItemName(), lost.getTitle(), lost.getContent())
		);
	}

	public FoundItemContext precompute(FoundItem found) {
		return new FoundItemContext(
				found,
				tokenizer.tokenize(found.getFoundArea(), found.getFoundPlace(), found.getKeepPlace()),
				tokenizer.tokenize(found.getItemName()),
				tokenizer.tokenize(found.getContent()),
				normalizer.extractColor(found.getColorName(), found.getItemName(), found.getTitle(), found.getContent())
		);
	}

	// ─── Public score methods ─────────────────────────────────────────────────

	public MatchScoreResult score(LostItem lost, FoundItem found) {
		return score(precompute(lost), found);
	}

	public MatchScoreResult score(LostItemContext lostCtx, FoundItem found) {
		LostItem lost = lostCtx.item();
		if (lost == null || found == null) {
			return MatchScoreResult.ineligible("비교 대상이 없습니다.");
		}
		if (lost.isDeleted() || found.isDeleted()) {
			return MatchScoreResult.ineligible("삭제된 게시글입니다.");
		}
		if (!isOpenLostStatus(lost.getStatus()) || !isOpenFoundStatus(found.getStatus())) {
			return MatchScoreResult.ineligible("매칭 대상 상태가 아닙니다.");
		}
		if (hasConflictingCategory(lost, found)) {
			return MatchScoreResult.ineligible("카테고리가 다릅니다.");
		}
		if (!isTimeEligible(lost, found)) {
			return MatchScoreResult.ineligible("분실/습득 시간이 매칭 범위를 벗어났습니다.");
		}

		Set<String> foundLocationTokens = tokenizer.tokenize(found.getFoundArea(), found.getFoundPlace(), found.getKeepPlace());
		Set<String> foundKeywordTokens = tokenizer.tokenize(found.getItemName());
		Set<String> foundDetailTokens = tokenizer.tokenize(found.getContent());
		Optional<String> foundColor = normalizer.extractColor(found.getColorName(), found.getItemName(), found.getTitle(), found.getContent());

		List<String> reasons = new ArrayList<>();
		BigDecimal categoryScore = categoryScore(lost, found, reasons);
		BigDecimal timeScore = timeScore(lost, found, reasons);
		BigDecimal locationScore = locationScore(lostCtx.locationTokens(), foundLocationTokens, reasons);
		BigDecimal keywordScore = keywordScore(lostCtx.keywordTokens(), foundKeywordTokens, reasons);
		BigDecimal colorScore = colorScore(lostCtx.color(), foundColor, reasons);
		BigDecimal detailScore = detailScore(lostCtx.detailTokens(), foundDetailTokens, reasons);

		BigDecimal total = categoryScore
				.add(timeScore)
				.add(locationScore)
				.add(keywordScore)
				.add(colorScore)
				.add(detailScore)
				.setScale(2, RoundingMode.HALF_UP);

		return new MatchScoreResult(
				true, total, categoryScore, timeScore, locationScore,
				keywordScore, colorScore, detailScore, List.copyOf(reasons)
		);
	}

	public MatchScoreResult score(LostItem lost, FoundItemContext foundCtx) {
		FoundItem found = foundCtx.item();
		if (lost == null || found == null) {
			return MatchScoreResult.ineligible("비교 대상이 없습니다.");
		}
		if (lost.isDeleted() || found.isDeleted()) {
			return MatchScoreResult.ineligible("삭제된 게시글입니다.");
		}
		if (!isOpenLostStatus(lost.getStatus()) || !isOpenFoundStatus(found.getStatus())) {
			return MatchScoreResult.ineligible("매칭 대상 상태가 아닙니다.");
		}
		if (hasConflictingCategory(lost, found)) {
			return MatchScoreResult.ineligible("카테고리가 다릅니다.");
		}
		if (!isTimeEligible(lost, found)) {
			return MatchScoreResult.ineligible("분실/습득 시간이 매칭 범위를 벗어났습니다.");
		}

		Set<String> lostLocationTokens = tokenizer.tokenize(lost.getLostArea(), lost.getLostPlace());
		Set<String> lostKeywordTokens = tokenizer.tokenize(lost.getItemName());
		Set<String> lostDetailTokens = tokenizer.tokenize(lost.getContent());
		Optional<String> lostColor = normalizer.extractColor(lost.getItemName(), lost.getTitle(), lost.getContent());

		List<String> reasons = new ArrayList<>();
		BigDecimal categoryScore = categoryScore(lost, found, reasons);
		BigDecimal timeScore = timeScore(lost, found, reasons);
		BigDecimal locationScore = locationScore(lostLocationTokens, foundCtx.locationTokens(), reasons);
		BigDecimal keywordScore = keywordScore(lostKeywordTokens, foundCtx.keywordTokens(), reasons);
		BigDecimal colorScore = colorScore(lostColor, foundCtx.color(), reasons);
		BigDecimal detailScore = detailScore(lostDetailTokens, foundCtx.detailTokens(), reasons);

		BigDecimal total = categoryScore
				.add(timeScore)
				.add(locationScore)
				.add(keywordScore)
				.add(colorScore)
				.add(detailScore)
				.setScale(2, RoundingMode.HALF_UP);

		return new MatchScoreResult(
				true, total, categoryScore, timeScore, locationScore,
				keywordScore, colorScore, detailScore, List.copyOf(reasons)
		);
	}

	// ─── Private helpers ──────────────────────────────────────────────────────

	private boolean isOpenLostStatus(String status) {
		return status == null || status.equals("WAITING");
	}

	private boolean isOpenFoundStatus(String status) {
		return status == null || status.equals("KEEPING");
	}

	private boolean hasConflictingCategory(LostItem lost, FoundItem found) {
		return StringUtils.hasText(lost.getCategoryMain())
				&& StringUtils.hasText(found.getCategoryMain())
				&& !lost.getCategoryMain().trim().equals(found.getCategoryMain().trim());
	}

	private boolean isTimeEligible(LostItem lost, FoundItem found) {
		if (lost.getLostAt() == null || found.getFoundAt() == null) {
			return true;
		}

		long hours = Duration.between(lost.getLostAt(), found.getFoundAt()).toHours();
		return hours >= -(LOST_DATE_MARGIN_DAYS * 24L) && hours <= (MAX_MATCH_DAYS * 24L);
	}

	private BigDecimal categoryScore(LostItem lost, FoundItem found, List<String> reasons) {
		boolean mainMissing = !StringUtils.hasText(lost.getCategoryMain()) || !StringUtils.hasText(found.getCategoryMain());
		if (mainMissing) {
			reasons.add("카테고리 일부 미입력");
			return CATEGORY_MISSING_SCORE;
		}

		boolean mainMatches = lost.getCategoryMain().trim().equals(found.getCategoryMain().trim());
		if (!mainMatches) {
			return BigDecimal.ZERO;
		}

		if (StringUtils.hasText(lost.getCategorySub()) && StringUtils.hasText(found.getCategorySub())
				&& lost.getCategorySub().trim().equals(found.getCategorySub().trim())) {
			reasons.add("세부 카테고리 일치 (" + lost.getCategorySub().trim() + ")");
			return CATEGORY_MAIN_AND_SUB_SCORE;
		}

		reasons.add("카테고리 일치 (" + lost.getCategoryMain().trim() + ")");
		return CATEGORY_MAIN_SCORE;
	}

	private BigDecimal timeScore(LostItem lost, FoundItem found, List<String> reasons) {
		if (lost.getLostAt() == null || found.getFoundAt() == null) {
			return BigDecimal.ZERO;
		}

		long hours = Duration.between(lost.getLostAt(), found.getFoundAt()).toHours();
		long days = Math.max(0, (long) Math.ceil(hours / 24.0));
		BigDecimal score;
		if (hours < 0) {
			score = new BigDecimal("12");
			reasons.add("습득일이 분실일보다 앞서지만 허용 범위 안입니다.");
		} else if (days <= 1) {
			score = new BigDecimal("25");
			reasons.add("분실/습득일 1일 이내");
		} else if (days <= 3) {
			score = new BigDecimal("20");
			reasons.add("분실/습득일 " + days + "일 차이");
		} else if (days <= 7) {
			score = new BigDecimal("14");
			reasons.add("분실/습득일 " + days + "일 차이");
		} else if (days <= 14) {
			score = new BigDecimal("8");
			reasons.add("분실/습득일 " + days + "일 차이");
		} else if (days <= 30) {
			score = new BigDecimal("4");
			reasons.add("분실/습득일 " + days + "일 차이");
		} else {
			score = BigDecimal.ONE;
		}
		return score;
	}

	private BigDecimal locationScore(Set<String> lostTokens, Set<String> foundTokens, List<String> reasons) {
		if (lostTokens.isEmpty() || foundTokens.isEmpty()) {
			return BigDecimal.ZERO;
		}

		long commonCount = lostTokens.stream().filter(foundTokens::contains).count();
		boolean transportCase = lostTokens.contains("이동수단") || foundTokens.contains("이동수단");

		BigDecimal score = BigDecimal.ZERO;
		if (commonCount > 0) {
			double ratio = (double) commonCount / Math.max(lostTokens.size(), foundTokens.size());
			score = LOCATION_WEIGHT.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
			reasons.add("장소 키워드 일치 (" + commonTerms(lostTokens, foundTokens) + ")");
		}
		if (transportCase && score.compareTo(TRANSPORT_LOCATION_FLOOR_SCORE) < 0) {
			score = TRANSPORT_LOCATION_FLOOR_SCORE;
			reasons.add("이동수단 분실 가능성");
		}
		return score.min(LOCATION_WEIGHT);
	}

	private BigDecimal keywordScore(Set<String> lostTokens, Set<String> foundTokens, List<String> reasons) {
		if (lostTokens.isEmpty() || foundTokens.isEmpty()) {
			return BigDecimal.ZERO;
		}

		BigDecimal score = weightedTokenScore(lostTokens, foundTokens, KEYWORD_WEIGHT);
		if (score.compareTo(BigDecimal.ZERO) > 0) {
			reasons.add("물품 키워드 일치 (" + commonTerms(lostTokens, foundTokens) + ")");
		}
		return score;
	}

	private BigDecimal colorScore(Optional<String> lostColor, Optional<String> foundColor, List<String> reasons) {
		if (lostColor.isPresent() && foundColor.isPresent()) {
			if (lostColor.get().equals(foundColor.get())) {
				reasons.add("색상 일치 (" + lostColor.get() + ")");
				return COLOR_MATCH_SCORE;
			}
			return BigDecimal.ZERO;
		}
		if (lostColor.isPresent() || foundColor.isPresent()) {
			return COLOR_PARTIAL_SCORE;
		}
		return BigDecimal.ZERO;
	}

	private BigDecimal detailScore(Set<String> lostTokens, Set<String> foundTokens, List<String> reasons) {
		if (lostTokens.isEmpty() || foundTokens.isEmpty()) {
			return BigDecimal.ZERO;
		}

		BigDecimal score = weightedTokenScore(lostTokens, foundTokens, DETAIL_WEIGHT);
		if (score.compareTo(BigDecimal.ZERO) > 0) {
			reasons.add("상세 설명 키워드 일치 (" + commonTerms(lostTokens, foundTokens) + ")");
		}
		return score;
	}

	private BigDecimal weightedTokenScore(Set<String> leftTokens, Set<String> rightTokens, BigDecimal weight) {
		double commonWeight = leftTokens.stream()
				.filter(rightTokens::contains)
				.mapToDouble(this::tokenWeight)
				.sum();
		if (commonWeight == 0.0) {
			return BigDecimal.ZERO;
		}
		double denominator = Math.max(totalTokenWeight(leftTokens), totalTokenWeight(rightTokens));
		double ratio = commonWeight / denominator;
		return weight.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
	}

	private double totalTokenWeight(Set<String> tokens) {
		return tokens.stream().mapToDouble(this::tokenWeight).sum();
	}

	private double tokenWeight(String token) {
		if (!StringUtils.hasText(token)) {
			return 0.0;
		}
		OptionalDouble configuredWeight = dictionary.configuredWeight(token);
		if (configuredWeight.isPresent()) {
			return configuredWeight.getAsDouble();
		}
		if (MODEL_TOKEN_PATTERN.matcher(token).matches()) {
			return 1.4;
		}
		if (isMixedCompactToken(token)) {
			return 0.7;
		}
		return 1.0;
	}

	private boolean isMixedCompactToken(String token) {
		boolean hasKorean = token.chars().anyMatch(ch -> ch >= '가' && ch <= '힣');
		boolean hasAsciiLetter = token.chars().anyMatch(ch -> ch >= 'a' && ch <= 'z');
		boolean hasDigit = token.chars().anyMatch(Character::isDigit);
		return hasKorean && (hasAsciiLetter || hasDigit);
	}

	private String commonTerms(Set<String> leftTokens, Set<String> rightTokens) {
		return String.join(", ", leftTokens.stream()
				.filter(rightTokens::contains)
				.limit(5)
				.toList());
	}
}
