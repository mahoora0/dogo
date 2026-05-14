package com.example.dogo.service.match;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class MatchTextNormalizer {

	private static final Map<String, String> COLOR_SYNONYMS = new LinkedHashMap<>();

	static {
		COLOR_SYNONYMS.put("검정색", "검정");
		COLOR_SYNONYMS.put("검은색", "검정");
		COLOR_SYNONYMS.put("까만색", "검정");
		COLOR_SYNONYMS.put("블랙", "검정");
		COLOR_SYNONYMS.put("black", "검정");
		COLOR_SYNONYMS.put("하얀색", "흰색");
		COLOR_SYNONYMS.put("흰색", "흰색");
		COLOR_SYNONYMS.put("화이트", "흰색");
		COLOR_SYNONYMS.put("white", "흰색");
		COLOR_SYNONYMS.put("회색", "회색");
		COLOR_SYNONYMS.put("그레이", "회색");
		COLOR_SYNONYMS.put("gray", "회색");
		COLOR_SYNONYMS.put("grey", "회색");
		COLOR_SYNONYMS.put("남색", "남색");
		COLOR_SYNONYMS.put("네이비", "남색");
		COLOR_SYNONYMS.put("곤색", "남색");
		COLOR_SYNONYMS.put("navy", "남색");
		COLOR_SYNONYMS.put("파란색", "파랑");
		COLOR_SYNONYMS.put("파랑", "파랑");
		COLOR_SYNONYMS.put("블루", "파랑");
		COLOR_SYNONYMS.put("blue", "파랑");
		COLOR_SYNONYMS.put("빨간색", "빨강");
		COLOR_SYNONYMS.put("빨강", "빨강");
		COLOR_SYNONYMS.put("레드", "빨강");
		COLOR_SYNONYMS.put("red", "빨강");
		COLOR_SYNONYMS.put("갈색", "갈색");
		COLOR_SYNONYMS.put("브라운", "갈색");
		COLOR_SYNONYMS.put("brown", "갈색");
		COLOR_SYNONYMS.put("베이지", "베이지");
		COLOR_SYNONYMS.put("beige", "베이지");
	}

	public String normalize(String text) {
		if (!StringUtils.hasText(text)) {
			return "";
		}

		String normalized = text.trim().toLowerCase();
		normalized = normalized.replaceAll("[\\[\\]{}()<>]", " ");
		normalized = normalized.replaceAll("[,./·|:;_\\-]+", " ");
		normalized = normalized.replaceAll("\\s+", " ");

		for (Map.Entry<String, String> entry : COLOR_SYNONYMS.entrySet()) {
			normalized = normalized.replace(entry.getKey(), entry.getValue());
		}
		return normalized.trim();
	}

	public Optional<String> extractColor(String... values) {
		String text = normalize(String.join(" ", valuesWithText(values)));
		if (!StringUtils.hasText(text)) {
			return Optional.empty();
		}

		for (String normalizedColor : COLOR_SYNONYMS.values()) {
			if (text.contains(normalizedColor)) {
				return Optional.of(normalizedColor);
			}
		}
		return Optional.empty();
	}

	private String[] valuesWithText(String... values) {
		return java.util.Arrays.stream(values)
				.filter(StringUtils::hasText)
				.toArray(String[]::new);
	}
}
