package com.example.dogo.service.match;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MatchTextTokenizer {

	private static final Pattern TOKEN_PART_PATTERN = Pattern.compile("[가-힣]+|[a-z]+|\\d+");
	private static final Pattern ALNUM_MODEL_PATTERN = Pattern.compile("[a-z]+\\d+[a-z0-9]*|\\d+[a-z]+[a-z0-9]*");
	private static final Set<String> TRANSPORT_TERMS = Set.of(
			"버스", "지하철", "택시", "기차", "열차", "ktx", "터미널", "공항", "비행기"
	);

	private final MatchTextNormalizer normalizer;
	private final MatchDictionary dictionary;

	public MatchTextTokenizer(MatchTextNormalizer normalizer, MatchDictionary dictionary) {
		this.normalizer = normalizer;
		this.dictionary = dictionary;
	}

	public Set<String> tokenize(String... values) {
		Set<String> tokens = new LinkedHashSet<>();
		String text = normalizer.normalize(String.join(" ", valuesWithText(values)));
		if (!StringUtils.hasText(text)) {
			return tokens;
		}

		Arrays.stream(text.split("[\\s]+"))
				.map(String::trim)
				.forEach(token -> {
					addToken(tokens, token);
					addPatternTokens(tokens, token, TOKEN_PART_PATTERN);
					addPatternTokens(tokens, token, ALNUM_MODEL_PATTERN);
				});

		dictionary.findDomainNouns(text).forEach(token -> addExpandedToken(tokens, token));
		new ArrayList<>(tokens).forEach(token -> dictionary.expand(token).forEach(expanded -> addToken(tokens, expanded)));

		for (String term : TRANSPORT_TERMS) {
			if (text.contains(term)) {
				tokens.add(term);
				tokens.add("이동수단");
			}
		}

		return tokens;
	}

	private void addExpandedToken(Set<String> tokens, String token) {
		addToken(tokens, token);
		dictionary.expand(token).forEach(expanded -> addToken(tokens, expanded));
	}

	private void addPatternTokens(Set<String> tokens, String value, Pattern pattern) {
		Matcher matcher = pattern.matcher(value);
		while (matcher.find()) {
			addToken(tokens, matcher.group());
		}
	}

	private void addToken(Set<String> tokens, String token) {
		if (!StringUtils.hasText(token)) {
			return;
		}
		if (token.length() >= 2 || token.chars().allMatch(Character::isDigit)) {
			tokens.add(token);
		}
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
