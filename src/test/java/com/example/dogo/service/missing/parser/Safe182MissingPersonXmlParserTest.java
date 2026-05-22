package com.example.dogo.service.missing.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Safe182MissingPersonXmlParserTest {

	private final Safe182MissingPersonXmlParser parser = new Safe182MissingPersonXmlParser();

	@Test
	void parsesFindChildListXmlResponse() {
		String xml = """
				<response>
					<result>00</result>
					<msg>OK</msg>
					<totalCount>1</totalCount>
					<list>
						<occrde>20260518</occrde>
						<nm>Hong Gil-dong</nm>
						<sexdstnDscd>male</sexdstnDscd>
						<age>13</age>
						<ageNow>13</ageNow>
						<occrAdres>Seoul Gangnam</occrAdres>
						<height>170</height>
						<bdwgh>58</bdwgh>
						<frmDscd>Slim</frmDscd>
						<faceshpeDscd>Oval</faceshpeDscd>
						<haircolrDscd>Black</haircolrDscd>
						<hairshpeDscd>Short</hairshpeDscd>
						<alldressingDscd>Blue hoodie</alldressingDscd>
					</list>
				</response>
				""";

		var page = parser.parse(xml);

		assertThat(page.result()).isEqualTo("00");
		assertThat(page.totalCount()).isEqualTo(1);
		assertThat(page.records()).hasSize(1);
		assertThat(page.records().get(0).externalId()).isNotBlank();
		assertThat(page.records().get(0).name()).isEqualTo("Hong Gil-dong");
		assertThat(page.records().get(0).occurredAt()).hasToString("2026-05-18T00:00");
		assertThat(page.records().get(0).occurredPlace()).isEqualTo("Seoul Gangnam");
		assertThat(page.records().get(0).heightCm()).isEqualTo(170);
		assertThat(page.records().get(0).weightKg()).isEqualByComparingTo("58.0");
	}

	@Test
	void parsesFindChildListXmlResponseWithEmptyHeightAndPhotoLength() {
		String xml = """
				<response>
					<result>00</result>
					<msg>OK</msg>
					<totalCount>1</totalCount>
					<list>
						<occrde>20260518</occrde>
						<nm>Hong Gil-dong</nm>
						<sexdstnDscd>male</sexdstnDscd>
						<age>13</age>
						<ageNow>13</ageNow>
						<occrAdres>Seoul Gangnam</occrAdres>
						<height></height>
						<bdwgh>58</bdwgh>
						<frmDscd>Slim</frmDscd>
						<faceshpeDscd>Oval</faceshpeDscd>
						<haircolrDscd>Black</haircolrDscd>
						<hairshpeDscd>Short</hairshpeDscd>
						<alldressingDscd>Blue hoodie</alldressingDscd>
						<tknphotolength>1234</tknphotolength>
					</list>
				</response>
				""";

		var page = parser.parse(xml);

		assertThat(page.records()).hasSize(1);
		assertThat(page.records().get(0).heightCm()).isNull();
	}

	@Test
	void parsesFindChildListXmlResponseWithLiteralNullValues() {
		String xml = """
				<response>
					<result>00</result>
					<msg>null</msg>
					<totalCount>1</totalCount>
					<list>
						<occrde>20260518</occrde>
						<nm>Hong Gil-dong</nm>
						<sexdstnDscd>male</sexdstnDscd>
						<age>null</age>
						<occrAdres>null</occrAdres>
						<height>null</height>
						<bdwgh>null</bdwgh>
						<frmDscd>null</frmDscd>
						<faceshpeDscd>null</faceshpeDscd>
						<haircolrDscd>null</haircolrDscd>
						<hairshpeDscd>null</hairshpeDscd>
						<alldressingDscd>null</alldressingDscd>
					</list>
				</response>
				""";

		var page = parser.parse(xml);

		assertThat(page.records()).hasSize(1);
		var record = page.records().get(0);
		assertThat(record.age()).isNull();
		assertThat(record.occurredPlace()).isNull();
		assertThat(record.heightCm()).isNull();
		assertThat(record.weightKg()).isNull();
		assertThat(record.bodyType()).isNull();
		assertThat(record.faceShape()).isNull();
		assertThat(record.hairColor()).isNull();
		assertThat(record.hairStyle()).isNull();
		assertThat(record.clothing()).isNull();
	}
}
