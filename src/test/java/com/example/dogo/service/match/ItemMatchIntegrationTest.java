package com.example.dogo.service.match;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class ItemMatchIntegrationTest {

    @Autowired
    private ItemMatchScorer itemMatchScorer;

    @Autowired
    private LostItemRepository lostItemRepository;

    @Autowired
    private FoundItemRepository foundItemRepository;

    @Test
    @DisplayName("유사한 단어(빨간 vs 빨간색)가 포함된 물건 매칭 테스트")
    void testSimilarityMatching() {
        // 1. 분실물 등록 (빨간 지갑)
        LostItem lost = new LostItem(
                null,
                "빨간 지갑 잃어버렸어요",
                "어제 강남역 근처에서 잃어버린 빨간 지갑입니다.",
                "지갑",
                "지갑",
                "반지갑",
                "레드",
                LocalDateTime.now().minusDays(1),
                "서울특별시 강남구",
                "강남역 2번 출구",
                "010-1234-5678"
        );
        lostItemRepository.save(lost);

        // 2. 습득물 등록 (빨간색 지갑)
        FoundItem found = new FoundItem(
                null,
                "강남역 인근에서 빨간색 지갑 습득",
                "지갑",
                "지갑",
                "반지갑",
                LocalDateTime.now(),
                "서울특별시 강남구",
                "강남역 인근",
                "강남역 파출소",
                "빨간색",
                "빨간색 가죽 지갑입니다.",
                "02-123-4567"
        );
        foundItemRepository.save(found);

        // 3. 유사도 점수 계산 실행
        MatchScoreResult result = itemMatchScorer.score(lost, found);

        // 4. 결과 출력 및 검증
        System.out.println("========================================");
        System.out.println("매칭 점수: " + result.totalScore());
        System.out.println("매칭 사유: " + result.reasons());
        System.out.println("========================================");

        // 점수가 임계값(보통 50점 이상)을 넘는지 확인
        assertThat(result.totalScore()).isGreaterThan(new java.math.BigDecimal("50.00"));
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("물품명 유사도 높음"));
    }
}
