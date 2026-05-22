package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Safe182AmberAlertXmlParserTest {

	private final Safe182AmberAlertXmlParser parser = new Safe182AmberAlertXmlParser();

	@Test
	void parsesAmberAlertXmlResponse() {
		String xml = """
				<response>
					<result>00</result>
					<msg>OK</msg>
					<totalCount>1</totalCount>
					<list>
						<occrde>20260518</occrde>
						<alldressingDscd>파란색 상의</alldressingDscd>
						<ageNow>12</ageNow>
						<age>11</age>
						<writngTrgetDscd>010</writngTrgetDscd>
						<sexdstnDscd>남자</sexdstnDscd>
						<occrAdres>서울특별시 종로구</occrAdres>
						<nm>홍길동</nm>
						<height>145</height>
						<bdwgh>38</bdwgh>
						<frmDscd>보통</frmDscd>
						<faceshpeDscd>계란형</faceshpeDscd>
						<hairshpeDscd>짧은머리</hairshpeDscd>
						<haircolrDscd>흑색</haircolrDscd>
						<tknphotolength>1234</tknphotolength>
					</list>
				</response>
				""";

		Safe182AmberAlertPage page = parser.parse(xml);

		assertThat(page.resultCode()).isEqualTo("00");
		assertThat(page.resultMessage()).isEqualTo("OK");
		assertThat(page.totalCount()).isEqualTo(1);
		assertThat(page.alerts()).hasSize(1);
		assertThat(page.alerts().get(0).name()).isEqualTo("홍길동");
		assertThat(page.alerts().get(0).occurrenceDate()).isEqualTo("2026.05.18");
		assertThat(page.alerts().get(0).targetType()).isEqualTo("정상아동(18세미만)");
		assertThat(page.alerts().get(0).occurrenceAddress()).isEqualTo("서울특별시 종로구");
		assertThat(page.alerts().get(0).sourceLabel()).isEqualTo("자료 출처: 경찰청");
	}

	@Test
	void parsesAmberAlertXmlResponseWithLiteralNullValues() {
		String xml = """
				<response>
					<result>null</result>
					<msg>null</msg>
					<totalCount>1</totalCount>
					<list>
						<occrde>null</occrde>
						<alldressingDscd>null</alldressingDscd>
						<ageNow>null</ageNow>
						<age>null</age>
						<writngTrgetDscd>null</writngTrgetDscd>
						<sexdstnDscd>null</sexdstnDscd>
						<occrAdres>null</occrAdres>
						<nm>null</nm>
						<height>null</height>
						<bdwgh>null</bdwgh>
						<frmDscd>null</frmDscd>
						<faceshpeDscd>null</faceshpeDscd>
						<hairshpeDscd>null</hairshpeDscd>
						<haircolrDscd>null</haircolrDscd>
						<tknphotolength>null</tknphotolength>
					</list>
				</response>
				""";

		Safe182AmberAlertPage page = parser.parse(xml);

		assertThat(page.resultCode()).isNull();
		assertThat(page.resultMessage()).isNull();
		assertThat(page.totalCount()).isEqualTo(1);
		assertThat(page.alerts()).hasSize(1);
		var alert = page.alerts().get(0);
		assertThat(alert.name()).isNull();
		assertThat(alert.occurrenceDate()).isNull();
		assertThat(alert.occurrenceAddress()).isNull();
		assertThat(alert.dressing()).isNull();
		assertThat(alert.ageNow()).isNull();
		assertThat(alert.age()).isNull();
		assertThat(alert.targetType()).isNull();
		assertThat(alert.sex()).isNull();
		assertThat(alert.height()).isNull();
		assertThat(alert.weight()).isNull();
		assertThat(alert.bodyType()).isNull();
		assertThat(alert.faceShape()).isNull();
		assertThat(alert.hairStyle()).isNull();
		assertThat(alert.hairColor()).isNull();
		assertThat(alert.photoLength()).isNull();
	}
}
