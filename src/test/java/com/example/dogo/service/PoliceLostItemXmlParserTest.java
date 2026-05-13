package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.dto.PoliceLostItemPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PoliceLostItemXmlParserTest {

	private final PoliceLostItemXmlParser parser = new PoliceLostItemXmlParser();

	@Test
	void parsesLostItemXmlResponse() {
		String xml = """
				<response>
					<header>
						<resultCode>00</resultCode>
						<resultMag>NORMAL SERVICE</resultMag>
					</header>
					<body>
						<items>
							<item>
								<atcId>L202605090000001</atcId>
								<lstPlace>강남역</lstPlace>
								<lstPrdtNm>노트북 가방</lstPrdtNm>
								<lstSbjt>검정 가방을 찾습니다</lstSbjt>
								<lstYmd>2026-05-08</lstYmd>
								<prdtClNm>가방 &gt; 기타가방</prdtClNm>
								<rnum>1</rnum>
							</item>
							<item>
								<atcId>L202605090000002</atcId>
								<lstPlace>서울역</lstPlace>
								<lstPrdtNm>카드 지갑</lstPrdtNm>
								<lstSbjt>지갑 분실</lstSbjt>
								<lstYmd>2026-05-09</lstYmd>
								<prdtClNm>지갑 &gt; 카드지갑</prdtClNm>
								<rnum>2</rnum>
							</item>
						</items>
						<numOfRows>100</numOfRows>
						<pageNo>1</pageNo>
						<totalCount>2</totalCount>
					</body>
				</response>
				""";

		PoliceLostItemPage page = parser.parse(xml);

		assertThat(page.resultCode()).isEqualTo("00");
		assertThat(page.resultMessage()).isEqualTo("NORMAL SERVICE");
		assertThat(page.totalCount()).isEqualTo(2);
		assertThat(page.items()).hasSize(2);
		assertThat(page.items().get(0).atcId()).isEqualTo("L202605090000001");
		assertThat(page.items().get(0).prdtClNm()).isEqualTo("가방 > 기타가방");
		assertThat(page.items().get(1).lstPlace()).isEqualTo("서울역");
	}

	@Test
	void parsesLostItemDetailXmlResponse() {
		String xml = """
				<response>
					<header>
						<resultCode>00</resultCode>
						<resultMsg>NORMAL SERVICE.</resultMsg>
					</header>
					<body>
						<items>
							<item>
								<lstPrdtNm>루이까또즈 남성용 반지갑</lstPrdtNm>
								<atcId>L2018120100000706</atcId>
								<lstYmd>2018-12-01</lstYmd>
								<lstHor>21</lstHor>
								<lstPlace>대흥동 택시 안</lstPlace>
								<prdtClNm>지갑 &gt; 남성용 지갑</prdtClNm>
								<lstSteNm>담당자 접수</lstSteNm>
								<uniq>개인정보보호정책에 의해 정보가 제공되지 않습니다.</uniq>
								<lstFilePathImg>
									https://minwon24.police.go.kr/images/sub/img02_no_img.gif
								</lstFilePathImg>
								<clrNm>블루(파랑)</clrNm>
								<lstLctNm>대전광역시</lstLctNm>
								<lstSbjt>루이까또즈 남성용 반지갑(블루(파랑)색)을 분실하였습니다.</lstSbjt>
								<orgId>O0000673</orgId>
								<orgNm>대전역지구대</orgNm>
								<tel>042-271-0112</tel>
								<lstPlaceSeNm>택시</lstPlaceSeNm>
							</item>
						</items>
					</body>
				</response>
				""";

		PoliceLostItemDetailResponse detail = parser.parseDetail(xml).orElseThrow();

		assertThat(detail.atcId()).isEqualTo("L2018120100000706");
		assertThat(detail.lstPrdtNm()).isEqualTo("루이까또즈 남성용 반지갑");
		assertThat(detail.lstHor()).isEqualTo("21");
		assertThat(detail.uniq()).isEqualTo("개인정보보호정책에 의해 정보가 제공되지 않습니다.");
		assertThat(detail.clrNm()).isEqualTo("블루(파랑)");
		assertThat(detail.lstLctNm()).isEqualTo("대전광역시");
		assertThat(detail.orgNm()).isEqualTo("대전역지구대");
		assertThat(detail.tel()).isEqualTo("042-271-0112");
		assertThat(detail.lstFilePathImg()).isEqualTo("https://minwon24.police.go.kr/images/sub/img02_no_img.gif");
	}
}
