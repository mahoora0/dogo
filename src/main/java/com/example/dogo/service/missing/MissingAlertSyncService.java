package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;
import com.example.dogo.dto.missing.Safe182AmberAlertView;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.stream.Stream;

@Service
public class MissingAlertSyncService {

	private static final Logger log = LoggerFactory.getLogger(MissingAlertSyncService.class);
	private static final String API_PROVIDER = "SAFE182_AMBER";
	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter DASHED_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final MissingAlertService missingAlertService;
	private final MissingPersonRepository missingPersonRepository;
	private final int rowSize;
	private final int backfillLookbackDays;
	private final int incrementalLookbackDays;

	public MissingAlertSyncService(
			MissingAlertService missingAlertService,
			MissingPersonRepository missingPersonRepository,
			@Value("${safe182.missing-alert.num-of-rows:100}") int rowSize,
			@Value("${safe182.missing-alert.backfill-lookback-days:1}") int backfillLookbackDays,
			@Value("${safe182.missing-alert.incremental-lookback-days:7}") int incrementalLookbackDays
	) {
		this.missingAlertService = missingAlertService;
		this.missingPersonRepository = missingPersonRepository;
		this.rowSize = Math.max(1, Math.min(rowSize, 100));
		this.backfillLookbackDays = Math.max(backfillLookbackDays, 1);
		this.incrementalLookbackDays = Math.max(incrementalLookbackDays, 1);
	}

	public MissingAlertSyncResult syncBackfillFromToday() {
		return syncBackfill(LocalDate.now());
	}

	public MissingAlertSyncResult syncIncrementalFromToday() {
		return syncRange(LocalDate.now().minusDays(incrementalLookbackDays - 1L), LocalDate.now());
	}

	MissingAlertSyncResult syncBackfill(LocalDate endDate) {
		return syncRange(endDate.minusDays(backfillLookbackDays), endDate);
	}

	MissingAlertSyncResult syncDate(LocalDate date) {
		int fetchedCount = 0;
		int savedCount = 0;
		int skippedCount = 0;
		int pageCount = 0;
		int pageNumber = 1;

		while (true) {
			Safe182AmberAlertPage page = missingAlertService.fetchAlerts(rowSize, pageNumber, date);
			pageCount++;

			if (page.alerts().isEmpty()) {
				break;
			}

			for (Safe182AmberAlertView alert : page.alerts()) {
				fetchedCount++;
				if (saveIfNew(alert, date)) {
					savedCount++;
				} else {
					skippedCount++;
				}
			}

			if (page.totalCount() <= pageNumber * rowSize || page.alerts().size() < rowSize) {
				break;
			}
			pageNumber++;
		}

		return new MissingAlertSyncResult(fetchedCount, savedCount, skippedCount, pageCount);
	}

	private MissingAlertSyncResult syncRange(LocalDate startDate, LocalDate endDate) {
		MissingAlertSyncResult result = new MissingAlertSyncResult(0, 0, 0, 0);
		LocalDate current = startDate;
		while (!current.isAfter(endDate)) {
			result = result.plus(syncDate(current));
			current = current.plusDays(1);
		}
		return result;
	}

	private boolean saveIfNew(Safe182AmberAlertView alert, LocalDate requestedDate) {
		if (alert == null) {
			return false;
		}

		String externalId = externalId(alert);
		if (missingPersonRepository.existsByApiProviderAndExternalId(API_PROVIDER, externalId)) {
			return false;
		}

		try {
			MissingPersonReport report = MissingPersonReport.fromPublicApi(
					API_PROVIDER,
					externalId,
					rawPayload(alert),
					parseAge(firstText(alert.ageNow(), alert.age())),
					"대한민국",
					parseOccurredAt(alert.occurrenceDate(), requestedDate),
					fallback(alert.occurrenceAddress(), "장소 미상"),
					parseInteger(alert.height()),
					parseDecimal(alert.weight()),
					fallback(alert.bodyType(), "미상"),
					fallback(alert.faceShape(), "미상"),
					fallback(alert.hairColor(), "미상"),
					fallback(alert.hairStyle(), "미상"),
					fallback(alert.dressing(), "착의 정보 없음")
			);
			missingPersonRepository.save(report);
			return true;
		} catch (DataIntegrityViolationException exception) {
			log.debug("이미 저장된 안전Dream 실종 경보입니다. externalId={}", externalId, exception);
			return false;
		} catch (IllegalArgumentException exception) {
			log.warn("안전Dream 실종 경보를 게시판 글로 저장하지 못했습니다. externalId={}, reason={}", externalId, exception.getMessage());
			return false;
		}
	}

	private String externalId(Safe182AmberAlertView alert) {
		String canonical = String.join("|",
				fallback(alert.occurrenceDate(), ""),
				fallback(alert.name(), ""),
				fallback(alert.occurrenceAddress(), ""),
				fallback(alert.ageNow(), ""),
				fallback(alert.age(), ""),
				fallback(alert.sex(), ""),
				fallback(alert.height(), ""),
				fallback(alert.weight(), ""),
				fallback(alert.dressing(), "")
		);
		return "safe182-" + sha256(canonical).substring(0, 32);
	}

	private String rawPayload(Safe182AmberAlertView alert) {
		try {
			return OBJECT_MAPPER.writeValueAsString(alert);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("실종 경보 원본 데이터를 JSON으로 변환하지 못했습니다.", exception);
		}
	}

	private LocalDateTime parseOccurredAt(String value, LocalDate fallbackDate) {
		LocalDate date = parseDate(value, fallbackDate);
		return LocalDateTime.of(date, LocalTime.MIDNIGHT);
	}

	private LocalDate parseDate(String value, LocalDate fallbackDate) {
		if (!StringUtils.hasText(value)) {
			return fallbackDate;
		}
		String trimmed = value.trim();
		for (DateTimeFormatter formatter : new DateTimeFormatter[] { BASIC_DATE, DASHED_DATE }) {
			try {
				return LocalDate.parse(trimmed, formatter);
			} catch (RuntimeException ignored) {
				// Try the next known Safe182 date shape.
			}
		}
		return fallbackDate;
	}

	private Integer parseAge(String value) {
		Integer parsed = parseInteger(value);
		if (parsed == null || parsed < 0) {
			return 0;
		}
		return parsed;
	}

	private Integer parseInteger(String value) {
		String digits = digitsAndDot(value);
		if (!StringUtils.hasText(digits)) {
			return null;
		}
		return new BigDecimal(digits).intValue();
	}

	private BigDecimal parseDecimal(String value) {
		String digits = digitsAndDot(value);
		if (!StringUtils.hasText(digits)) {
			return null;
		}
		return new BigDecimal(digits);
	}

	private String digitsAndDot(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String cleaned = value.trim().replaceAll("[^0-9.]", "");
		return StringUtils.hasText(cleaned) ? cleaned : null;
	}

	private String firstText(String... values) {
		return Stream.of(values)
				.filter(StringUtils::hasText)
				.map(String::trim)
				.findFirst()
				.orElse(null);
	}

	private String fallback(String value, String fallback) {
		return StringUtils.hasText(value) ? value.trim() : fallback;
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 해시를 사용할 수 없습니다.", exception);
		}
	}
}
