package com.example.dogo.service;

import com.example.dogo.dto.PoliceRegionCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PoliceCommonCodeXmlParserTest {

	private final PoliceCommonCodeXmlParser parser = new PoliceCommonCodeXmlParser();

	@Test
	void parsesRegionCodesAndPrefixesChildNamesWithTopLevelRegion() {
		String xml = """
				<response>
					<header>
						<resultCode>00</resultCode>
						<resultMsg>NORMAL SERVICE.</resultMsg>
					</header>
					<body>
						<items>
							<item>
								<cdNm>서울특별시</cdNm>
								<commCd>LCA000</commCd>
							</item>
							<item>
								<cdNm>용산구</cdNm>
								<commCd>LCA020</commCd>
							</item>
							<item>
								<cdNm>경기도</cdNm>
								<commCd>LCI000</commCd>
							</item>
							<item>
								<cdNm>가평군</cdNm>
								<commCd>LCI001</commCd>
							</item>
						</items>
					</body>
				</response>
				""";

		List<PoliceRegionCode> regionCodes = parser.parseRegionCodes(xml);

		assertThat(regionCodes).containsExactly(
				new PoliceRegionCode("LCA000", "서울특별시"),
				new PoliceRegionCode("LCA020", "서울특별시 용산구"),
				new PoliceRegionCode("LCI000", "경기도"),
				new PoliceRegionCode("LCI001", "경기도 가평군")
		);
	}
}
