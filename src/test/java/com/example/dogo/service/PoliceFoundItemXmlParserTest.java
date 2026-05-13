package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.PoliceFoundItemPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PoliceFoundItemXmlParserTest {

	private final PoliceFoundItemXmlParser parser = new PoliceFoundItemXmlParser();

	@Test
	void parsesFoundItemXmlResponse() {
		String xml = """
				<response>
					<header>
						<resultCode>00</resultCode>
						<resultMag>NORMAL SERVICE</resultMag>
					</header>
					<body>
						<items>
							<item>
								<atcId>F202605110000001</atcId>
								<clrNm>브라운(갈)</clrNm>
								<depPlace>서울역(한국철도공사)</depPlace>
								<fdFilePathImg>https://example.com/found/bread.jpg</fdFilePathImg>
								<fdPrdtNm>빵 봉투</fdPrdtNm>
								<fdSbjt>빵 봉투(브라운(갈)색)을 습득하여 보관하고 있습니다</fdSbjt>
								<fdSn>1</fdSn>
								<fdYmd>2026-05-11</fdYmd>
								<prdtClNm>쇼핑백 &gt; 쇼핑백</prdtClNm>
								<rnum>1</rnum>
							</item>
						</items>
						<totalCount>1</totalCount>
					</body>
				</response>
				""";

		PoliceFoundItemPage page = parser.parse(xml);

		assertThat(page.resultCode()).isEqualTo("00");
		assertThat(page.resultMessage()).isEqualTo("NORMAL SERVICE");
		assertThat(page.totalCount()).isEqualTo(1);
		assertThat(page.items()).hasSize(1);
		assertThat(page.items().get(0).atcId()).isEqualTo("F202605110000001");
		assertThat(page.items().get(0).fdSn()).isEqualTo("1");
		assertThat(page.items().get(0).prdtClNm()).isEqualTo("쇼핑백 > 쇼핑백");
	}

	@Test
	void ignoresEmptyItemNodeInZeroResultResponse() {
		String xml = """
				<response>
					<header>
						<resultCode>00</resultCode>
						<resultMsg>NORMAL SERVICE</resultMsg>
					</header>
					<body>
						<items>
							<item />
						</items>
						<totalCount>0</totalCount>
					</body>
				</response>
				""";

		PoliceFoundItemPage page = parser.parse(xml);

		assertThat(page.totalCount()).isZero();
		assertThat(page.items()).isEmpty();
	}

	@Test
	void parsesFoundItemDetailXmlResponse() {
		String xml = """
				<response>
					<header>
						<resultCode>00</resultCode>
						<resultMsg>NORMAL SERVICE</resultMsg>
					</header>
					<body>
						<items>
							<item>
								<atcId>F202605110000001</atcId>
								<csteSteNm>보관중</csteSteNm>
								<depPlace>서울역(한국철도공사)</depPlace>
								<fdFilePathImg>https://example.com/found/bread.jpg</fdFilePathImg>
								<fdHor>20</fdHor>
								<fdPlace>기차</fdPlace>
								<fdPrdtNm>빵 봉투</fdPrdtNm>
								<fdSn>1</fdSn>
								<fdYmd>2026-05-11</fdYmd>
								<fndKeepOrgnSeNm>기관보관</fndKeepOrgnSeNm>
								<orgId>O0000001</orgId>
								<orgNm>서울역(한국철도공사)</orgNm>
								<prdtClNm>쇼핑백 &gt; 쇼핑백</prdtClNm>
								<tel>02-3149-2531</tel>
								<uniq>특이사항 : 없음</uniq>
								<clrNm>브라운(갈)</clrNm>
							</item>
						</items>
					</body>
				</response>
				""";

		PoliceFoundItemDetailResponse detail = parser.parseDetail(xml).orElseThrow();

		assertThat(detail.atcId()).isEqualTo("F202605110000001");
		assertThat(detail.fdHor()).isEqualTo("20");
		assertThat(detail.fdPlace()).isEqualTo("기차");
		assertThat(detail.orgNm()).isEqualTo("서울역(한국철도공사)");
		assertThat(detail.tel()).isEqualTo("02-3149-2531");
		assertThat(detail.uniq()).isEqualTo("특이사항 : 없음");
	}
}
