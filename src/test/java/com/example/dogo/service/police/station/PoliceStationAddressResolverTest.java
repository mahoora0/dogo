package com.example.dogo.service.police.station;

import com.example.dogo.dto.police.PoliceFoundItemDetailResponse;
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
	void resolvesDuplicateStationNameWithOrgIdAndTelAlias() {
		assertThat(resolver.resolveFoundArea(detail("O0001689", "061-760-0151"), "전라남도"))
				.contains("전남, 광양시");
		assertThat(resolver.resolveFoundArea(detail("O0001727", "061-860-7301"), "전라남도"))
				.contains("전남, 장흥군");
	}

	@Test
	void skipsAmbiguousStationNameWithoutOfficeOrRegion() {
		Optional<String> foundArea = resolver.resolveFoundArea("중앙지구대", null, null);

		assertThat(foundArea).isEmpty();
	}

	private PoliceFoundItemDetailResponse detail(String orgId, String tel) {
		return new PoliceFoundItemDetailResponse(
				"F2026051300003911",
				"보관중",
				"읍내지구대",
				null,
				"23",
				"노상",
				"삼성핸드폰",
				"1",
				"2026-05-13",
				"관서보관",
				orgId,
				"읍내지구대",
				"휴대폰 > 삼성휴대폰",
				tel,
				"특이사항 : 없음",
				"블랙(검정)"
		);
	}
}
