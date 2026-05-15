package com.example.dogo.service.match.semantic;

import com.example.dogo.service.match.embedding.ItemEmbeddingService;
import com.example.dogo.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "match.embedding.enabled", havingValue = "true")
public class PrecomputedSemanticMatchClient implements SemanticMatchClient {

	private static final Logger log = LoggerFactory.getLogger(PrecomputedSemanticMatchClient.class);

	private final ItemEmbeddingService embeddingService;
	private final SemanticMatchTextBuilder textBuilder;

	public PrecomputedSemanticMatchClient(ItemEmbeddingService embeddingService,
			SemanticMatchTextBuilder textBuilder) {
		this.embeddingService = embeddingService;
		this.textBuilder = textBuilder;
	}

	@Override
	public SemanticMatchResponse score(SemanticMatchRequest request) {
		String queryText = textBuilder.build(request.query());
		if (!StringUtils.hasText(queryText)) {
			return new SemanticMatchResponse(null, List.of());
		}

		// 1. query 실시간 임베딩 (1개)
		float[] queryVector;
		try {
			queryVector = embeddingService.getOrFetchOne(request.query().type(), request.query());
		} catch (Exception e) {
			log.warn("[precomputed] query 임베딩 조회 실패, rule-only 폴백: {}", e.getMessage());
			return new SemanticMatchResponse(null, List.of());
		}
		if (queryVector.length == 0) {
			return new SemanticMatchResponse(null, List.of());
		}

		// 2. candidates: DB 조회 + 없으면 Python 호출해서 저장
		String candidateType = request.candidates().get(0).type();
		Map<Long, float[]> embeddingMap;
		try {
			embeddingMap = embeddingService.getOrFetch(candidateType, request.candidates());
		} catch (Exception e) {
			log.warn("[precomputed] candidate 임베딩 조회 실패, rule-only 폴백: {}", e.getMessage());
			return new SemanticMatchResponse(null, List.of());
		}

		// 3. Java cosine 계산 (정규화된 벡터 → 내적 = 코사인)
		List<SemanticMatchResult> results = request.candidates().stream()
				.filter(c -> embeddingMap.containsKey(c.id()))
				.map(c -> {
					float cosine = VectorUtils.cosineSimilarity(queryVector, embeddingMap.get(c.id()));
					BigDecimal score = BigDecimal.valueOf(Math.max(0.0, Math.min(100.0, cosine * 100.0)))
							.setScale(2, RoundingMode.HALF_UP);
					List<String> reasons = score.compareTo(new BigDecimal("50")) >= 0
							? List.of("물품명/제목/카테고리/색상 의미 유사") : List.of();
					return new SemanticMatchResult(c.id(), score, reasons);
				})
				.toList();

		log.debug("[precomputed] 코사인 계산 완료: query={} candidates={} scored={}",
				queryText, request.candidates().size(), results.size());

		return new SemanticMatchResponse(null, results);
	}
}
