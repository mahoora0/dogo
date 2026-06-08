package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiRecord;
import com.example.dogo.entity.animal.AnimalReport;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AnimalPublicApiMapper {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)");

	public AnimalReport toReport(AnimalPublicApiRecord record, String apiProvider, String reportType) {
		String finalReportType = reportType;
		if ("ANIMAL_PROTECTION_API".equals(apiProvider)) {
			String state = record.processState();
			if (state != null && state.contains("반환")) {
				finalReportType = "RETURNED";
			} else {
				finalReportType = "TRANSFERRED";
			}
		}

		return AnimalReport.fromPublicApi(
				apiProvider,
				record.externalId(),
				finalReportType,
				title(record, finalReportType),
				eventDate(record.eventDate()),
				null,
				null,
				defaultText(record.regionName(), "UNKNOWN"),
				defaultText(record.eventPlace(), "UNKNOWN"),
				record.careTel(),
				true,
				"SIGHTING".equals(finalReportType) ? "PROTECTING" : null,
				animalType(record),
				breedName(record),
				gender(record.sexCode()),
				neuteredStatus(record.neuterCode()),
				null,
				null,
				weight(record.weightText()),
				record.color(),
				record.feature(),
				content(record),
				record.rawPayload()
		);
	}

	private String title(AnimalPublicApiRecord record, String reportType) {
		String subject = firstText(breedName(record), "MISSING".equals(reportType) ? "실종 동물" : "구조 동물");
		if ("MISSING".equals(reportType)) {
			return subject + " 실종 신고";
		}
		if ("구조 동물".equals(subject) && StringUtils.hasText(record.externalId())) {
			return subject + " " + record.externalId().trim();
		}
		return subject + " 구조 신고";
	}

	private String content(AnimalPublicApiRecord record) {
		StringBuilder builder = new StringBuilder();
		append(builder, "상태", record.processState());
		append(builder, "특징", record.feature());
		append(builder, "나이", record.ageText());
		append(builder, "체중", record.weightText());
		return builder.isEmpty() ? null : builder.toString();
	}

	private String animalType(AnimalPublicApiRecord record) {
		String text = defaultText(record.kindName(), record.breedName()).toLowerCase(Locale.ROOT);
		if (text.contains("cat") || text.contains("고양")) {
			return "CAT";
		}
		if (text.contains("dog") || text.contains("개") || text.contains("견")) {
			return "DOG";
		}
		return "OTHER";
	}

	private String breedName(AnimalPublicApiRecord record) {
		String breed = record.breedName();
		if (StringUtils.hasText(breed)) {
			String normalizedBreed = breed.trim();
			return isCodeOnly(normalizedBreed) ? null : normalizedBreed;
		}
		String kind = record.kindName();
		if (!StringUtils.hasText(kind)) {
			return null;
		}
		String normalized = kind.trim();
		int close = normalized.indexOf(']');
		if (close >= 0 && close + 1 < normalized.length()) {
			normalized = normalized.substring(close + 1).trim();
		}
		if (!StringUtils.hasText(normalized) || isCodeOnly(normalized)) {
			return null;
		}
		return normalized;
	}

	private boolean isCodeOnly(String value) {
		return value != null && value.trim().matches("\\d+");
	}

	private String gender(String value) {
		if (!StringUtils.hasText(value)) {
			return "UNKNOWN";
		}
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "M", "MALE" -> "MALE";
			case "F", "FEMALE" -> "FEMALE";
			default -> "UNKNOWN";
		};
	}

	private String neuteredStatus(String value) {
		if (!StringUtils.hasText(value)) {
			return "UNKNOWN";
		}
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "Y", "YES", "NEUTERED" -> "NEUTERED";
			case "N", "NO", "NOT_NEUTERED" -> "NOT_NEUTERED";
			default -> "UNKNOWN";
		};
	}

	private LocalDate eventDate(String value) {
		if (!StringUtils.hasText(value)) {
			return LocalDate.now();
		}
		String normalized = value.trim().replace("-", "");
		if (normalized.length() >= 8) {
			return LocalDate.parse(normalized.substring(0, 8), BASIC_DATE);
		}
		return LocalDate.now();
	}

	private BigDecimal weight(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		Matcher matcher = WEIGHT_PATTERN.matcher(value);
		if (!matcher.find()) {
			return null;
		}
		return new BigDecimal(matcher.group(1).replace(',', '.'));
	}

	private String firstText(String... values) {
		for (String value : values) {
			if (StringUtils.hasText(value)) {
				return value.trim();
			}
		}
		return "Animal";
	}

	private String defaultText(String value, String fallback) {
		return StringUtils.hasText(value) ? value.trim() : fallback;
	}

	private void append(StringBuilder builder, String label, String value) {
		if (!StringUtils.hasText(value)) {
			return;
		}
		if (!builder.isEmpty()) {
			builder.append('\n');
		}
		builder.append(label).append(": ").append(value.trim());
	}
}
