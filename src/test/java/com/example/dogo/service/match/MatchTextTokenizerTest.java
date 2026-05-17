package com.example.dogo.service.match;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MatchTextTokenizerTest {

	private final MatchTextTokenizer tokenizer = new MatchTextTokenizer(new MatchTextNormalizer(), new MatchDictionary());

	@Test
	void tokenizeSplitsCompactPhoneModelTerms() {
		Set<String> tokens = tokenizer.tokenize("아이폰15프로");

		assertThat(tokens).contains("아이폰", "iphone", "15", "프로", "pro");
	}

	@Test
	void tokenizeKeepsAlphaNumericModelTokenAndParts() {
		Set<String> tokens = tokenizer.tokenize("갤럭시s24울트라");

		assertThat(tokens).contains("갤럭시", "galaxy", "s24", "24", "울트라", "ultra");
	}
}
