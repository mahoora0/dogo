package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class Safe182AmberAlertRestClient implements Safe182AmberAlertClient {

	private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final RestClient restClient;
	private final Safe182AmberAlertXmlParser parser;
	private final String esntlId;
	private final String authKey;

	public Safe182AmberAlertRestClient(
			@Value("${safe182.amber-list.url}") String amberListUrl,
			@Value("${safe182.esntl-id:}") String esntlId,
			@Value("${safe182.auth-key:}") String authKey,
			Safe182AmberAlertXmlParser parser
	) {
		this.restClient = RestClient.builder()
				.baseUrl(amberListUrl)
				.build();
		this.esntlId = esntlId;
		this.authKey = authKey;
		this.parser = parser;
	}

	@Override
	public Safe182AmberAlertPage fetchAlerts(LocalDate occurrenceDate, int rowSize, Integer page) {
		if (!StringUtils.hasText(esntlId) || !StringUtils.hasText(authKey)) {
			throw new IllegalStateException("안전Dream 실종경보 API 인증 정보가 설정되지 않았습니다.");
		}

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("esntlId", esntlId);
		form.add("authKey", authKey);
		form.add("rowSize", String.valueOf(rowSize));
		form.add("occrde", occurrenceDate.format(REQUEST_DATE_FORMAT));
		form.add("xmlUseYN", "Y");
		if (page != null) {
			form.add("page", String.valueOf(page));
		}

		String response = utf8(restClient.post()
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(byte[].class));

		Safe182AmberAlertPage alertPage = parser.parse(response);
		if (StringUtils.hasText(alertPage.resultCode()) && !"00".equals(alertPage.resultCode())) {
			throw new IllegalStateException("안전Dream 실종경보 API 호출 실패: "
					+ alertPage.resultCode() + " " + alertPage.resultMessage());
		}
		return alertPage;
	}

	private String utf8(byte[] response) {
		if (response == null || response.length == 0) {
			return "";
		}
		return new String(response, StandardCharsets.UTF_8);
	}
}
