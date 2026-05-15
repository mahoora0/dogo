package com.example.dogo.service.match;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class ItemMatchScorer {

    private static final int LOST_DATE_MARGIN_DAYS = 3;
    private static final int MAX_MATCH_DAYS = 60;

    // 가중치 설정
    private static final BigDecimal CATEGORY_MAIN_AND_SUB_SCORE = new BigDecimal("15");
    private static final BigDecimal CATEGORY_MAIN_SCORE = new BigDecimal("10");
    private static final BigDecimal LOCATION_WEIGHT = new BigDecimal("20");
    private static final BigDecimal KEYWORD_WEIGHT = new BigDecimal("30"); // 물품명 유사도 가중치 상향
    private static final BigDecimal COLOR_MATCH_SCORE = new BigDecimal("10");
    private static final BigDecimal DETAIL_WEIGHT = new BigDecimal("15");

    private final MatchTextNormalizer normalizer;
    private final MatchTextTokenizer tokenizer;
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public ItemMatchScorer(MatchTextNormalizer normalizer, MatchTextTokenizer tokenizer) {
        this.normalizer = normalizer;
        this.tokenizer = tokenizer;
    }

    public MatchScoreResult score(LostItem lost, FoundItem found) {
        if (lost == null || found == null) return MatchScoreResult.ineligible("비교 대상이 없습니다.");
        if (lost.isDeleted() || found.isDeleted()) return MatchScoreResult.ineligible("삭제된 게시글입니다.");
        
        // 1. 카테고리 체크 (대분류가 다르면 즉시 탈락)
        if (hasConflictingCategory(lost, found)) {
            return MatchScoreResult.ineligible("카테고리가 다릅니다.");
        }

        // 2. 시간 범위 체크
        if (!isTimeEligible(lost, found)) {
            return MatchScoreResult.ineligible("날짜 범위를 벗어났습니다.");
        }

        List<String> reasons = new ArrayList<>();
        
        // 항목별 점수 계산
        BigDecimal categoryScore = calculateCategoryScore(lost, found, reasons);
        BigDecimal timeScore = calculateTimeScore(lost, found, reasons);
        BigDecimal locationScore = calculateLocationScore(lost, found, reasons);
        BigDecimal keywordScore = calculateSimilarityScore(lost.getItemName(), found.getItemName(), KEYWORD_WEIGHT, "물품명", reasons);
        BigDecimal colorScore = calculateColorScore(lost, found, reasons);
        BigDecimal detailScore = calculateSimilarityScore(lost.getContent(), found.getContent(), DETAIL_WEIGHT, "상세내용", reasons);

        BigDecimal total = categoryScore.add(timeScore).add(locationScore).add(keywordScore).add(colorScore).add(detailScore)
                .setScale(2, RoundingMode.HALF_UP);

        return new MatchScoreResult(true, total, categoryScore, timeScore, locationScore, keywordScore, colorScore, detailScore, List.copyOf(reasons));
    }

    private boolean hasConflictingCategory(LostItem lost, FoundItem found) {
        return StringUtils.hasText(lost.getCategoryMain()) && StringUtils.hasText(found.getCategoryMain())
                && !lost.getCategoryMain().trim().equals(found.getCategoryMain().trim());
    }

    private boolean isTimeEligible(LostItem lost, FoundItem found) {
        if (lost.getLostAt() == null || found.getFoundAt() == null) return true;
        long hours = Duration.between(lost.getLostAt(), found.getFoundAt()).toHours();
        return hours >= -(LOST_DATE_MARGIN_DAYS * 24L) && hours <= (MAX_MATCH_DAYS * 24L);
    }

    private BigDecimal calculateCategoryScore(LostItem lost, FoundItem found, List<String> reasons) {
        if (lost.getCategoryMain().equals(found.getCategoryMain())) {
            if (StringUtils.hasText(lost.getCategorySub()) && lost.getCategorySub().equals(found.getCategorySub())) {
                reasons.add("세부 카테고리 일치");
                return CATEGORY_MAIN_AND_SUB_SCORE;
            }
            reasons.add("대분류 카테고리 일치");
            return CATEGORY_MAIN_SCORE;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTimeScore(LostItem lost, FoundItem found, List<String> reasons) {
        long days = Math.abs(Duration.between(lost.getLostAt(), found.getFoundAt()).toDays());
        if (days <= 1) { reasons.add("날짜 근접(1일 이내)"); return new BigDecimal("10"); }
        if (days <= 3) { reasons.add("날짜 근접(3일 이내)"); return new BigDecimal("7"); }
        return new BigDecimal("3");
    }

    private BigDecimal calculateLocationScore(LostItem lost, FoundItem found, List<String> reasons) {
        // 장소 키워드 토큰 매칭
        Set<String> lostTokens = tokenizer.tokenize(lost.getLostArea(), lost.getLostPlace());
        Set<String> foundTokens = tokenizer.tokenize(found.getFoundArea(), found.getFoundPlace());
        
        long matchCount = lostTokens.stream().filter(foundTokens::contains).count();
        if (matchCount > 0) {
            reasons.add("장소 키워드 일치 (" + matchCount + "건)");
            return LOCATION_WEIGHT;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateSimilarityScore(String text1, String text2, BigDecimal weight, String label, List<String> reasons) {
        if (!StringUtils.hasText(text1) || !StringUtils.hasText(text2)) return BigDecimal.ZERO;
        
        // Jaro-Winkler 유사도 알고리즘 사용 (빨간 vs 빨간색 대응 가능)
        double sim = similarity.apply(text1, text2);
        if (sim > 0.7) {
            reasons.add(label + " 유사도 높음 (" + Math.round(sim * 100) + "%)");
            return weight.multiply(BigDecimal.valueOf(sim)).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateColorScore(LostItem lost, FoundItem found, List<String> reasons) {
        Optional<String> lostColor = normalizer.extractColor(lost.getItemName(), lost.getTitle(), lost.getContent());
        Optional<String> foundColor = normalizer.extractColor(found.getColorName(), found.getItemName(), found.getTitle(), found.getContent());

        if (lostColor.isPresent() && foundColor.isPresent() && lostColor.get().equals(foundColor.get())) {
            reasons.add("색상 일치 (" + lostColor.get() + ")");
            return COLOR_MATCH_SCORE;
        }
        return BigDecimal.ZERO;
    }
}
