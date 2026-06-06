package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnimalPublicApiXmlParserTest {

	private final AnimalPublicApiXmlParser parser = new AnimalPublicApiXmlParser();

	@Test
	void parsesProtectionResponse() {
		String xml = """
				<response>
				  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
				  <body>
				    <items>
				      <item>
				        <desertionNo>PROTECT-1</desertionNo>
				        <happenDt>20260516</happenDt>
				        <happenPlace>Mapo shelter road</happenPlace>
				        <kindCd>[Dog] Poodle</kindCd>
				        <colorCd>White</colorCd>
				        <sexCd>F</sexCd>
				        <neuterYn>Y</neuterYn>
				        <specialMark>Blue collar</specialMark>
				        <careAddr>Seoul Mapo-gu</careAddr>
				        <careTel>02-111-2222</careTel>
				        <popfile>https://example.test/protect.jpg</popfile>
				      </item>
				    </items>
				    <totalCount>1</totalCount>
				  </body>
				</response>
				""";

		AnimalPublicApiPage page = parser.parse(xml);

		assertThat(page.resultCode()).isEqualTo("00");
		assertThat(page.totalCount()).isEqualTo(1);
		assertThat(page.records()).hasSize(1);
		assertThat(page.records().get(0).externalId()).isEqualTo("PROTECT-1");
		assertThat(page.records().get(0).eventDate()).isEqualTo("20260516");
		assertThat(page.records().get(0).kindName()).isEqualTo("[Dog] Poodle");
		assertThat(page.records().get(0).imageUrl()).isEqualTo("https://example.test/protect.jpg");
	}

	@Test
	void parsesProtectionResponseWithV2ImageFields() {
		String xml = """
				<response>
				  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
				  <body>
				    <items>
				      <item>
				        <desertionNo>PROTECT-2</desertionNo>
				        <happenDt>20260601</happenDt>
				        <happenPlace>Mapo shelter road</happenPlace>
				        <kindFullNm>[Dog] Maltese</kindFullNm>
				        <popfile1>https://example.test/protect-main.jpg</popfile1>
				        <popfile2>https://example.test/protect-sub.jpg</popfile2>
				      </item>
				    </items>
				    <totalCount>1</totalCount>
				  </body>
				</response>
				""";

		AnimalPublicApiPage page = parser.parse(xml);

		assertThat(page.records()).hasSize(1);
		assertThat(page.records().get(0).imageUrl()).isEqualTo("https://example.test/protect-main.jpg");
	}

	@Test
	void parsesLossResponse() {
		String xml = """
				<response>
				  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
				  <body>
				    <items>
				      <item>
				        <lossNo>LOSS-1</lossNo>
				        <lossDate>20260515</lossDate>
				        <lossPlace>Gangnam station</lossPlace>
				        <kindNm>Cat</kindNm>
				        <breedNm>Korean shorthair</breedNm>
				        <colorCd>Black</colorCd>
				        <sexCd>M</sexCd>
				        <neuterYn>N</neuterYn>
				        <feature>Green eyes</feature>
				        <orgNm>Seoul Gangnam-gu</orgNm>
				        <tel>02-333-4444</tel>
				        <filename>https://example.test/loss.jpg</filename>
				      </item>
				    </items>
				    <totalCount>1</totalCount>
				  </body>
				</response>
				""";

		AnimalPublicApiPage page = parser.parse(xml);

		assertThat(page.records()).hasSize(1);
		assertThat(page.records().get(0).externalId()).isEqualTo("LOSS-1");
		assertThat(page.records().get(0).eventDate()).isEqualTo("20260515");
		assertThat(page.records().get(0).eventPlace()).isEqualTo("Gangnam station");
		assertThat(page.records().get(0).breedName()).isEqualTo("Korean shorthair");
		assertThat(page.records().get(0).feature()).isEqualTo("Green eyes");
	}

	@Test
	void parsesLossResponseWithImageFilenameFallback() {
		String xml = """
				<response>
				  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
				  <body>
				    <items>
				      <item>
				        <lossDate>20260515</lossDate>
				        <lossPlace>Gangnam station</lossPlace>
				        <filename>https://example.test/images/20260521225018919.jpg</filename>
				      </item>
				    </items>
				    <totalCount>1</totalCount>
				  </body>
				</response>
				""";

		AnimalPublicApiPage page = parser.parse(xml);

		assertThat(page.records()).hasSize(1);
		// 이미지 파일명인 20260521225018919가 externalId로 추출되어야 함
		assertThat(page.records().get(0).externalId()).isEqualTo("20260521225018919");
	}

	@Test
	void parsesLossResponseWithSyntheticIdFallback() {
		String xml = """
				<response>
				  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
				  <body>
				    <items>
				      <item>
				        <lossDate>20260515</lossDate>
				        <tel>02-333-4444</tel>
				        <kindNm>Cat</kindNm>
				        <lossPlace>Gangnam station</lossPlace>
				      </item>
				    </items>
				    <totalCount>1</totalCount>
				  </body>
				</response>
				""";

		AnimalPublicApiPage page = parser.parse(xml);

		assertThat(page.records()).hasSize(1);
		// SYNTHETIC- 해시코드 형식의 고유 ID가 생성되어야 함
		assertThat(page.records().get(0).externalId()).startsWith("SYNTHETIC-");
	}
}
