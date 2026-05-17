package com.example.dogo.service.match;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchDictionaryTest {

	private final MatchDictionary dictionary = new MatchDictionary();

	@Test
	void loadsResourceDictionaryTermsAndExpansions() {
		assertThat(dictionary.domainNouns()).contains("아이패드", "ipad", "차키");
		assertThat(dictionary.expand("아이패드")).contains("아이패드", "ipad", "태블릿");
		assertThat(dictionary.configuredWeight("아이패드")).hasValue(1.6);
	}

	@Test
	void findsLongestDomainNounBeforeNestedNoun() {
		assertThat(dictionary.findDomainNouns("검정카드지갑")).contains("카드지갑");
		assertThat(dictionary.findDomainNouns("검정카드지갑")).doesNotContain("지갑");
	}
}
