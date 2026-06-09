package com.example.dogo.service.animal;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.animal.AnimalReportMatch;
import com.example.dogo.repository.animal.AnimalReportMatchRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "match.pet.embedding.enabled", havingValue = "true")
public class AnimalMatchService {

	private static final Logger log = LoggerFactory.getLogger(AnimalMatchService.class);
	private static final int DATE_RANGE_DAYS = 60;
	private static final int MAX_CANDIDATES = 10;
	private static final double MIN_SCORE = 40.0;

	private final AnimalReportRepository reportRepository;
	private final AnimalReportMatchRepository matchRepository;
	private final AnimalImageEmbeddingService embeddingService;

	public AnimalMatchService(
			AnimalReportRepository reportRepository,
			AnimalReportMatchRepository matchRepository,
			AnimalImageEmbeddingService embeddingService
	) {
		this.reportRepository = reportRepository;
		this.matchRepository = matchRepository;
		this.embeddingService = embeddingService;
	}

	@Transactional
	public void matchForReport(AnimalReport report) {
		MatchRunResult result;
		if ("MISSING".equals(report.getReportType())) {
			result = matchMissing(report);
		} else {
			result = matchSighting(report);
		}
		if (!result.replaceExisting()) {
			return;
		}
		if ("MISSING".equals(report.getReportType())) {
			matchRepository.deleteByMissingReport_ReportId(report.getReportId());
		} else {
			matchRepository.deleteBySightingReport_ReportId(report.getReportId());
		}
		matchRepository.saveAll(result.matches());
		matchRepository.flush();
	}

	private MatchRunResult matchMissing(AnimalReport missing) {
		LocalDate from = missing.getEventDate().minusDays(DATE_RANGE_DAYS);
		LocalDate to = missing.getEventDate().plusDays(DATE_RANGE_DAYS);

		List<AnimalReport> sightings = reportRepository.findSightingCandidates(
				missing.getAnimalType(), from, to);

		if (sightings.isEmpty()) {
			log.info("[pet-match] 후보 없음: missingId={}", missing.getReportId());
			return MatchRunResult.replaceWith(List.of());
		}

		Map<Long, float[]> queryVectors = embeddingService.loadVectors(List.of(missing.getReportId()));
		float[] queryVec = queryVectors.get(missing.getReportId());
		if (queryVec == null) {
			log.info("[pet-match] 쿼리 벡터 없음 (이미지 미등록), 스킵: missingId={}", missing.getReportId());
			return MatchRunResult.keepExisting();
		}

		List<Long> sightingIds = sightings.stream().map(AnimalReport::getReportId).toList();
		Map<Long, float[]> candidateVectors = embeddingService.loadVectors(sightingIds);

		if (candidateVectors.isEmpty()) {
			log.info("[pet-match] 후보 벡터 없음, 기존 매칭 유지: missingId={}", missing.getReportId());
			return MatchRunResult.keepExisting();
		}

		List<AnimalReportMatch> matches = sightings.stream()
				.filter(s -> candidateVectors.containsKey(s.getReportId()))
				.map(s -> {
					float cosine = VectorUtils.cosineSimilarity(queryVec, candidateVectors.get(s.getReportId()));
					double score = Math.max(0.0, Math.min(100.0, cosine * 100.0));
					return new ScoredCandidate(s, score);
				})
				.filter(sc -> sc.score() >= MIN_SCORE)
				.sorted((a, b) -> Double.compare(b.score(), a.score()))
				.limit(MAX_CANDIDATES)
				.map(sc -> buildMatch(missing, sc.report(), sc.score()))
				.toList();

		log.info("[pet-match] 매칭 완료: missingId={}, 후보={}건", missing.getReportId(), matches.size());
		return MatchRunResult.replaceWith(matches);
	}

	private MatchRunResult matchSighting(AnimalReport sighting) {
		LocalDate from = sighting.getEventDate().minusDays(DATE_RANGE_DAYS);
		LocalDate to = sighting.getEventDate().plusDays(DATE_RANGE_DAYS);

		List<AnimalReport> missings = reportRepository.findMissingCandidates(
				sighting.getAnimalType(), from, to);

		if (missings.isEmpty()) {
			log.info("[pet-match] 후보 없음: sightingId={}", sighting.getReportId());
			return MatchRunResult.replaceWith(List.of());
		}

		Map<Long, float[]> queryVectors = embeddingService.loadVectors(List.of(sighting.getReportId()));
		float[] queryVec = queryVectors.get(sighting.getReportId());
		if (queryVec == null) {
			log.info("[pet-match] 쿼리 벡터 없음 (이미지 미등록), 스킵: sightingId={}", sighting.getReportId());
			return MatchRunResult.keepExisting();
		}

		List<Long> missingIds = missings.stream().map(AnimalReport::getReportId).toList();
		Map<Long, float[]> candidateVectors = embeddingService.loadVectors(missingIds);

		if (candidateVectors.isEmpty()) {
			log.info("[pet-match] 후보 벡터 없음, 기존 매칭 유지: sightingId={}", sighting.getReportId());
			return MatchRunResult.keepExisting();
		}

		List<AnimalReportMatch> matches = missings.stream()
				.filter(m -> candidateVectors.containsKey(m.getReportId()))
				.map(m -> {
					float cosine = VectorUtils.cosineSimilarity(queryVec, candidateVectors.get(m.getReportId()));
					double score = Math.max(0.0, Math.min(100.0, cosine * 100.0));
					return new ScoredCandidate(m, score);
				})
				.filter(sc -> sc.score() >= MIN_SCORE)
				.sorted((a, b) -> Double.compare(b.score(), a.score()))
				.limit(MAX_CANDIDATES)
				.map(sc -> buildMatch(sc.report(), sighting, sc.score()))
				.toList();

		log.info("[pet-match] 매칭 완료: sightingId={}, 후보={}건", sighting.getReportId(), matches.size());
		return MatchRunResult.replaceWith(matches);
	}

	private AnimalReportMatch buildMatch(AnimalReport missing, AnimalReport sighting, double score) {
		BigDecimal finalScore = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
		return new AnimalReportMatch(missing, sighting, finalScore, embeddingService.currentModelName());
	}

	private record ScoredCandidate(AnimalReport report, double score) {}

	private record MatchRunResult(boolean replaceExisting, List<AnimalReportMatch> matches) {
		static MatchRunResult keepExisting() {
			return new MatchRunResult(false, List.of());
		}

		static MatchRunResult replaceWith(List<AnimalReportMatch> matches) {
			return new MatchRunResult(true, matches);
		}
	}
}
