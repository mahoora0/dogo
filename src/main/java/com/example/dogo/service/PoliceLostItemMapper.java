package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemResponse;
import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.entity.LostItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class PoliceLostItemMapper {

	private static final String UNKNOWN_PLACE = "장소 미상";

	public LostItem toLostItem(PoliceLostItemResponse response) {
		if (response == null) {
			throw new IllegalArgumentException("경찰청 분실물 응답이 비어 있습니다.");
		}
		if (!StringUtils.hasText(response.atcId())) {
			throw new IllegalArgumentException("경찰청 분실물 접수 ID가 없습니다.");
		}

		CategoryNames categoryNames = parseCategory(response.prdtClNm());
		String itemName = defaultText(response.lstPrdtNm(), response.lstSbjt());

		if (!StringUtils.hasText(itemName)) {
			throw new IllegalArgumentException("경찰청 분실물 물품명이 없습니다.");
		}

		return LostItem.fromPolice(
				response.atcId().trim(),
				defaultText(response.lstSbjt(), itemName),
				null,
				itemName,
				categoryNames.main(),
				categoryNames.sub(),
				colorName(response.lstSbjt(), response.lstPrdtNm(), null),
				parseLostAt(response.lstYmd()),
				null,
				defaultText(response.lstPlace(), UNKNOWN_PLACE),
				null
		);
	}

	public LostItem toLostItem(PoliceLostItemResponse listResponse, PoliceLostItemDetailResponse detailResponse) {
		if (detailResponse == null) {
			return toLostItem(listResponse);
		}

		String atcId = defaultText(detailResponse.atcId(), listResponse == null ? null : listResponse.atcId());
		if (!StringUtils.hasText(atcId)) {
			throw new IllegalArgumentException("경찰청 분실물 접수 ID가 없습니다.");
		}

		String itemName = defaultText(
				detailResponse.lstPrdtNm(),
				listResponse == null ? null : listResponse.lstPrdtNm()
		);
		itemName = defaultText(itemName, detailResponse.lstSbjt());
		if (!StringUtils.hasText(itemName)) {
			throw new IllegalArgumentException("경찰청 분실물 물품명이 없습니다.");
		}

		String category = defaultText(
				detailResponse.prdtClNm(),
				listResponse == null ? null : listResponse.prdtClNm()
		);
		CategoryNames categoryNames = parseCategory(category);

		return LostItem.fromPolice(
				atcId,
				defaultText(detailResponse.lstSbjt(), listResponse == null ? itemName : defaultText(listResponse.lstSbjt(), itemName)),
				blankToNull(detailResponse.uniq()),
				itemName,
				categoryNames.main(),
				categoryNames.sub(),
				colorName(detailResponse.lstSbjt(), detailResponse.lstPrdtNm(), detailResponse.clrNm()),
				parseLostAt(defaultText(detailResponse.lstYmd(), listResponse == null ? null : listResponse.lstYmd()), detailResponse.lstHor()),
				blankToNull(detailResponse.lstLctNm()),
				defaultText(detailResponse.lstPlace(), listResponse == null ? UNKNOWN_PLACE : defaultText(listResponse.lstPlace(), UNKNOWN_PLACE)),
				contact(detailResponse.orgNm(), detailResponse.tel())
		);
	}

	CategoryNames parseCategory(String prdtClNm) {
		if (!StringUtils.hasText(prdtClNm)) {
			return new CategoryNames(null, null);
		}

		String[] parts = prdtClNm.split(">");
		String main = blankToNull(parts[0]);
		String sub = parts.length > 1 ? blankToNull(parts[1]) : null;
		return new CategoryNames(main, sub);
	}

	private LocalDateTime parseLostAt(String lstYmd) {
		return parseLostAt(lstYmd, null);
	}

	private LocalDateTime parseLostAt(String lstYmd, String lstHor) {
		if (!StringUtils.hasText(lstYmd)) {
			throw new IllegalArgumentException("경찰청 분실물 분실일이 없습니다.");
		}

		String value = lstYmd.trim();
		try {
			LocalDate lostDate;
			if (value.matches("\\d{8}")) {
				lostDate = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
			} else {
				lostDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
			}
			return LocalDateTime.of(lostDate, parseLostTime(lstHor));
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("경찰청 분실물 분실일 형식이 올바르지 않습니다: " + lstYmd, exception);
		}
	}

	private LocalTime parseLostTime(String lstHor) {
		if (!StringUtils.hasText(lstHor)) {
			return LocalTime.MIDNIGHT;
		}

		String value = lstHor.trim();
		try {
			if (value.matches("\\d{1,2}")) {
				int hour = Integer.parseInt(value);
				if (hour >= 0 && hour <= 23) {
					return LocalTime.of(hour, 0);
				}
			}
			if (value.matches("\\d{1,2}:\\d{2}")) {
				return LocalTime.parse(value);
			}
		} catch (RuntimeException ignored) {
			return LocalTime.MIDNIGHT;
		}
		return LocalTime.MIDNIGHT;
	}

	private String contact(String orgNm, String tel) {
		String normalizedOrg = blankToNull(orgNm);
		String normalizedTel = blankToNull(tel);
		if (normalizedOrg != null && normalizedTel != null) {
			return normalizedOrg + " / " + normalizedTel;
		}
		if (normalizedTel != null) {
			return normalizedTel;
		}
		return normalizedOrg;
	}

	private String colorName(String title, String itemName, String detailColorName) {
		String normalizedDetailColor = normalizeColorName(detailColorName);
		if (normalizedDetailColor != null) {
			return normalizedDetailColor;
		}

		String text = String.join(" ", defaultText(title, ""), defaultText(itemName, ""));
		String colorInParentheses = extractColorInParentheses(text);
		if (colorInParentheses != null) {
			return colorInParentheses;
		}

		return normalizeColorName(text);
	}

	private String extractColorInParentheses(String text) {
		String normalizedText = blankToNull(text);
		if (normalizedText == null) {
			return null;
		}

		java.util.regex.Matcher matcher = java.util.regex.Pattern
				.compile("\\(([^)]*(?:색|검정|파랑|블랙|블루|화이트|레드|그린|브라운|그레이|핑크|노랑|초록|갈색|회색)[^)]*)\\)색?")
				.matcher(normalizedText);
		while (matcher.find()) {
			String value = normalizeColorName(matcher.group(1));
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private String normalizeColorName(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}

		String lower = normalized.toLowerCase();
		if (lower.contains("블랙") || lower.contains("검정") || lower.contains("검은") || lower.contains("black")) {
			return "블랙(검정)";
		}
		if (lower.contains("화이트") || lower.contains("흰색") || lower.contains("하얀") || lower.contains("white")) {
			return "화이트(흰색)";
		}
		if (lower.contains("블루") || lower.contains("파랑") || lower.contains("파란") || lower.contains("blue")) {
			return "블루(파랑)";
		}
		if (lower.contains("레드") || lower.contains("빨강") || lower.contains("빨간") || lower.contains("red")) {
			return "레드(빨강)";
		}
		if (lower.contains("그린") || lower.contains("초록") || lower.contains("green")) {
			return "그린(초록)";
		}
		if (lower.contains("옐로") || lower.contains("노랑") || lower.contains("노란") || lower.contains("yellow")) {
			return "옐로우(노랑)";
		}
		if (lower.contains("핑크") || lower.contains("분홍") || lower.contains("pink")) {
			return "핑크(분홍)";
		}
		if (lower.contains("브라운") || lower.contains("갈색") || lower.contains("brown")) {
			return "브라운(갈색)";
		}
		if (lower.contains("그레이") || lower.contains("회색") || lower.contains("gray") || lower.contains("grey")) {
			return "그레이(회색)";
		}
		if (lower.contains("베이지") || lower.contains("beige")) {
			return "베이지";
		}
		if (lower.contains("실버") || lower.contains("은색") || lower.contains("silver")) {
			return "실버(은색)";
		}
		if (lower.contains("골드") || lower.contains("금색") || lower.contains("gold")) {
			return "골드(금색)";
		}
		return normalized.contains("(") ? normalized : null;
	}

	private String defaultText(String value, String fallback) {
		String normalized = blankToNull(value);
		if (normalized != null) {
			return normalized;
		}
		return blankToNull(fallback);
	}

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	record CategoryNames(String main, String sub) {
	}
}
