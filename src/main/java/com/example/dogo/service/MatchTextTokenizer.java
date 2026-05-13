package com.example.dogo.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MatchTextTokenizer {

	private static final Map<String, List<String>> TERM_EXPANSIONS = new LinkedHashMap<>();
	private static final Set<String> TRANSPORT_TERMS = Set.of(
			"버스", "지하철", "택시", "기차", "열차", "ktx", "터미널", "공항", "비행기"
	);

	static {
		TERM_EXPANSIONS.put("지갑", List.of("지갑"));
		TERM_EXPANSIONS.put("반지갑", List.of("반지갑", "지갑"));
		TERM_EXPANSIONS.put("카드지갑", List.of("카드지갑", "지갑"));
		TERM_EXPANSIONS.put("장지갑", List.of("장지갑", "지갑"));
		TERM_EXPANSIONS.put("루에브르", List.of("루에브르"));
		TERM_EXPANSIONS.put("아이폰", List.of("아이폰", "iphone"));
		TERM_EXPANSIONS.put("iphone", List.of("아이폰", "iphone"));
		TERM_EXPANSIONS.put("갤럭시", List.of("갤럭시", "galaxy"));
		TERM_EXPANSIONS.put("galaxy", List.of("갤럭시", "galaxy"));
		TERM_EXPANSIONS.put("에어팟", List.of("에어팟", "airpods", "이어폰"));
		TERM_EXPANSIONS.put("airpods", List.of("에어팟", "airpods", "이어폰"));
		TERM_EXPANSIONS.put("노트북", List.of("노트북", "랩탑", "laptop"));
		TERM_EXPANSIONS.put("laptop", List.of("노트북", "랩탑", "laptop"));
		TERM_EXPANSIONS.put("백팩", List.of("백팩", "가방"));
		TERM_EXPANSIONS.put("배낭", List.of("배낭", "가방"));
	}

	private final MatchTextNormalizer normalizer;

	public MatchTextTokenizer(MatchTextNormalizer normalizer) {
		this.normalizer = normalizer;
	}

	public Set<String> tokenize(String... values) {
		Set<String> tokens = new LinkedHashSet<>();
		String text = normalizer.normalize(String.join(" ", valuesWithText(values)));
		if (!StringUtils.hasText(text)) {
			return tokens;
		}

		Arrays.stream(text.split("[\\s]+"))
				.map(String::trim)
				.filter(token -> token.length() >= 2)
				.forEach(tokens::add);

		for (Map.Entry<String, List<String>> entry : TERM_EXPANSIONS.entrySet()) {
			if (text.contains(entry.getKey())) {
				tokens.addAll(entry.getValue());
			}
		}

		for (String term : TRANSPORT_TERMS) {
			if (text.contains(term)) {
				tokens.add(term);
				tokens.add("이동수단");
			}
		}

		return tokens;
	}

	public boolean containsTransportTerm(String... values) {
		Set<String> tokens = tokenize(values);
		return tokens.contains("이동수단");
	}

	private String[] valuesWithText(String... values) {
		return Arrays.stream(values)
				.filter(StringUtils::hasText)
				.toArray(String[]::new);
	}
}
