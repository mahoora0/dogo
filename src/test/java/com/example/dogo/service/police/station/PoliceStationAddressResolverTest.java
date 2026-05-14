package com.example.dogo.service.police.station;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PoliceStationAddressResolverTest {

	private final PoliceStationAddressResolver resolver = new PoliceStationAddressResolver();

	@Test
	void resolvesDistrictAreaFromStationNameAndPoliceOffice() {
		Optional<String> foundArea = resolver.resolveFoundArea("삼전지구대", "서울송파경찰서", "서울특별시");

		assertThat(foundArea).contains("서울, 송파구");
	}

	@Test
	void resolvesWhenDetailPlaceAlreadyOmitsStationType() {
		Optional<String> foundArea = resolver.resolveFoundArea("삼전", "서울송파", "서울");

		assertThat(foundArea).contains("서울, 송파구");
	}

	@Test
	void resolvesAreaWhenDetailPlaceIsPoliceOfficeName() {
		assertThat(resolver.resolveFoundArea("서울송파경찰서", "서울송파경찰서", "서울특별시"))
				.contains("서울, 송파구");
		assertThat(resolver.resolveFoundArea("부산진경찰서", "부산진경찰서", "부산광역시"))
				.contains("부산, 부산진구");
	}

	@Test
	void resolvesDuplicateStationNameWithPoliceOffice() {
		Optional<String> foundArea = resolver.resolveFoundArea("읍내지구대", "보은경찰서", "충청북도");

		assertThat(foundArea).contains("충북, 보은군");
	}

	@Test
	void skipsAmbiguousStationNameWithoutOfficeOrRegion() {
		Optional<String> foundArea = resolver.resolveFoundArea("중앙지구대", null, null);

		assertThat(foundArea).isEmpty();
	}
}
