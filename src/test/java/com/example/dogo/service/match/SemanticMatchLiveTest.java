package com.example.dogo.service.match;

import com.example.dogo.entity.item.ItemMatch;
import com.example.dogo.repository.item.ItemMatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/**
 * 실제 MySQL + 실행 중인 Python 서버를 대상으로 하는 수동 검증용 테스트.
 * 평소 CI에서 돌리는 용도가 아니며, 실행 전 조건:
 *   1. MySQL 기동 (localhost:3306/dogo)
 *   2. Python 서버 기동 (localhost:8001)
 *   3. .env의 DB_USERNAME / DB_PASSWORD 환경변수 설정
 *
 * 실행: .\gradlew.bat test --tests "*.SemanticMatchLiveTest" -PuseMysql
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "useMysql", matches = "true")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/dogo?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8",
        "spring.datasource.username=root",
        "spring.datasource.password=1234",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.sql.init.mode=always",
        "match.semantic.enabled=true",
        "match.semantic.base-url=http://localhost:8001",
        "match.semantic.timeout-ms=60000",
        "police.lost-item.backfill-on-startup=false",
        "police.found-item.backfill-on-startup=false",
        "police.lost-item.sync.enabled=false",
        "police.found-item.sync.enabled=false",
})
class SemanticMatchLiveTest {

    @Autowired
    private ItemMatchService itemMatchService;

    @Autowired
    private ItemMatchRepository itemMatchRepository;

    @Test
    void matchForLostItem_withRealMysqlAndPython() throws Exception {
        // LOST_ID=31713 (카드지갑, 서울 강남구, 2026-05-13) — 후보 798개
        long lostId = 31713L;

        itemMatchService.matchForLostItemId(lostId);

        // 잠깐 대기 (@Async라서)
        Thread.sleep(5000);

        List<ItemMatch> matches = itemMatchRepository.findByLostIdWithFoundItem(lostId);
        System.out.println("\n=== 매칭 결과 (lostId=" + lostId + ") ===");
        System.out.printf("%-6s %-20s %-10s %-10s %-10s %-30s%n",
                "foundId", "itemName", "ruleScore", "semScore", "finalScore", "matchVersion");
        System.out.println("-".repeat(100));

        for (ItemMatch m : matches) {
            System.out.printf("%-6d %-20s %-10s %-10s %-10s %-30s%n",
                    m.getFoundItem().getFoundId(),
                    truncate(m.getFoundItem().getItemName(), 20),
                    m.getRuleScore(),
                    m.getSemanticScore(),
                    m.getFinalScore(),
                    m.getMatchVersion()
            );
        }

        boolean anyWithSemantic = matches.stream().anyMatch(m -> m.getSemanticScore() != null);
        System.out.println("\n시맨틱 점수 포함 여부: " + anyWithSemantic);
        System.out.println("총 매칭 수: " + matches.size());
    }

    private String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len - 1) + "…";
    }
}
