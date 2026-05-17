package com.example.dogo.service.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

@Component
public class MatchDictionary {

	private static final Logger log = LoggerFactory.getLogger(MatchDictionary.class);
	private static final String DICTIONARY_PATH = "data/match-dictionary.json";

	private final Set<String> domainNouns = new LinkedHashSet<>();
	private final Map<String, List<String>> expansions = new LinkedHashMap<>();
	private final Map<String, Double> tokenWeights = new LinkedHashMap<>();
	private List<String> longestFirstDomainNouns = List.of();

	public MatchDictionary() {
		loadFallbackDictionary();
		loadExternalDictionary();
		rebuildIndexes();
	}

	public Set<String> findDomainNouns(String text) {
		if (!StringUtils.hasText(text)) {
			return Set.of();
		}

		String normalized = normalize(text);
		Set<String> matches = new LinkedHashSet<>();
		boolean[] occupied = new boolean[normalized.length()];
		for (String noun : longestFirstDomainNouns) {
			int index = normalized.indexOf(noun);
			while (index >= 0) {
				int end = index + noun.length();
				if (!overlaps(occupied, index, end)) {
					matches.add(noun);
					for (int i = index; i < end; i++) {
						occupied[i] = true;
					}
					break;
				}
				index = normalized.indexOf(noun, index + 1);
			}
		}
		return matches;
	}

	public List<String> expand(String term) {
		if (!StringUtils.hasText(term)) {
			return List.of();
		}
		return expansions.getOrDefault(normalize(term), List.of());
	}

	public OptionalDouble configuredWeight(String token) {
		if (!StringUtils.hasText(token)) {
			return OptionalDouble.empty();
		}
		Double weight = tokenWeights.get(normalize(token));
		return weight == null ? OptionalDouble.empty() : OptionalDouble.of(weight);
	}

	Set<String> domainNouns() {
		return Collections.unmodifiableSet(domainNouns);
	}

	private void loadFallbackDictionary() {
		addDomainNouns(List.of(
				"지갑", "반지갑", "장지갑", "카드지갑", "동전지갑",
				"가방", "백팩", "크로스백", "핸드백", "쇼핑백", "여행가방", "파우치",
				"휴대폰", "스마트폰", "아이폰", "iphone", "갤럭시", "galaxy",
				"프로", "pro", "울트라", "ultra", "플러스", "plus", "미니", "mini",
				"에어팟", "airpods", "버즈", "buds", "이어폰",
				"노트북", "랩탑", "laptop", "맥북", "macbook", "태블릿",
				"신분증", "운전면허증", "학생증", "여권", "교통카드",
				"우산", "열쇠", "차키", "텀블러", "시계", "안경", "선글라스",
				"루에브르", "애플", "apple", "삼성", "samsung"
		));
		addExpansion("지갑", List.of("지갑"));
		addExpansion("반지갑", List.of("반지갑", "지갑"));
		addExpansion("카드지갑", List.of("카드지갑", "지갑"));
		addExpansion("장지갑", List.of("장지갑", "지갑"));
		addExpansion("아이폰", List.of("아이폰", "iphone"));
		addExpansion("iphone", List.of("아이폰", "iphone"));
		addExpansion("갤럭시", List.of("갤럭시", "galaxy"));
		addExpansion("galaxy", List.of("갤럭시", "galaxy"));
		addExpansion("프로", List.of("프로", "pro"));
		addExpansion("pro", List.of("프로", "pro"));
		addExpansion("울트라", List.of("울트라", "ultra"));
		addExpansion("ultra", List.of("울트라", "ultra"));
		addExpansion("플러스", List.of("플러스", "plus"));
		addExpansion("plus", List.of("플러스", "plus"));
		addExpansion("미니", List.of("미니", "mini"));
		addExpansion("mini", List.of("미니", "mini"));
		addExpansion("에어팟", List.of("에어팟", "airpods", "이어폰"));
		addExpansion("airpods", List.of("에어팟", "airpods", "이어폰"));
		addExpansion("버즈", List.of("버즈", "buds", "이어폰"));
		addExpansion("buds", List.of("버즈", "buds", "이어폰"));
		addExpansion("노트북", List.of("노트북", "랩탑", "laptop"));
		addExpansion("laptop", List.of("노트북", "랩탑", "laptop"));
		addExpansion("맥북", List.of("맥북", "macbook", "노트북", "랩탑", "laptop"));
		addExpansion("macbook", List.of("맥북", "macbook", "노트북", "랩탑", "laptop"));
		addExpansion("백팩", List.of("백팩", "가방"));
		addExpansion("배낭", List.of("배낭", "가방"));

		addWeights(Map.ofEntries(
				Map.entry("아이폰", 1.6), Map.entry("iphone", 1.6),
				Map.entry("갤럭시", 1.6), Map.entry("galaxy", 1.6),
				Map.entry("에어팟", 1.6), Map.entry("airpods", 1.6),
				Map.entry("버즈", 1.6), Map.entry("buds", 1.6),
				Map.entry("맥북", 1.6), Map.entry("macbook", 1.6),
				Map.entry("노트북", 1.6), Map.entry("laptop", 1.6),
				Map.entry("지갑", 1.4), Map.entry("반지갑", 1.5),
				Map.entry("카드지갑", 1.5), Map.entry("장지갑", 1.5),
				Map.entry("가방", 1.2), Map.entry("프로", 1.2),
				Map.entry("pro", 1.2), Map.entry("울트라", 1.2),
				Map.entry("ultra", 1.2), Map.entry("플러스", 1.2),
				Map.entry("plus", 1.2), Map.entry("미니", 1.2),
				Map.entry("mini", 1.2), Map.entry("루에브르", 1.6)
		));
	}

	private void loadExternalDictionary() {
		ClassPathResource resource = new ClassPathResource(DICTIONARY_PATH);
		if (!resource.exists()) {
			return;
		}

		try (InputStream inputStream = resource.getInputStream()) {
			DictionaryFile file = new ObjectMapper().readValue(inputStream, DictionaryFile.class);
			addDomainNouns(file.domainNouns());
			if (file.expansions() != null) {
				file.expansions().forEach(this::addExpansion);
			}
			addWeights(file.tokenWeights());
		} catch (IOException e) {
			log.warn("Failed to load {}. Falling back to built-in match dictionary.", DICTIONARY_PATH, e);
		}
	}

	private void addDomainNouns(List<String> nouns) {
		if (nouns == null) {
			return;
		}
		nouns.stream()
				.map(this::normalize)
				.filter(StringUtils::hasText)
				.forEach(domainNouns::add);
	}

	private void addExpansion(String term, List<String> values) {
		String key = normalize(term);
		if (!StringUtils.hasText(key) || values == null) {
			return;
		}
		List<String> normalizedValues = values.stream()
				.map(this::normalize)
				.filter(StringUtils::hasText)
				.distinct()
				.toList();
		if (!normalizedValues.isEmpty()) {
			expansions.put(key, normalizedValues);
			domainNouns.add(key);
			domainNouns.addAll(normalizedValues);
		}
	}

	private void addWeights(Map<String, Double> weights) {
		if (weights == null) {
			return;
		}
		weights.forEach((token, weight) -> {
			String key = normalize(token);
			if (StringUtils.hasText(key) && weight != null && weight > 0.0) {
				tokenWeights.put(key, weight);
			}
		});
	}

	private void rebuildIndexes() {
		longestFirstDomainNouns = domainNouns.stream()
				.sorted((left, right) -> Integer.compare(right.length(), left.length()))
				.toList();
	}

	private boolean overlaps(boolean[] occupied, int start, int end) {
		for (int i = start; i < end; i++) {
			if (occupied[i]) {
				return true;
			}
		}
		return false;
	}

	private String normalize(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}

	private record DictionaryFile(
			List<String> domainNouns,
			Map<String, List<String>> expansions,
			Map<String, Double> tokenWeights
	) {}
}
