package com.example.dogo.service.police.client;

import com.example.dogo.dto.police.PoliceRegionCode;
import com.example.dogo.service.police.parser.PoliceCommonCodeXmlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@Component
public class DataGoKrPoliceCommonCodeClient implements PoliceCommonCodeClient {

	private static final String REGION_GROUP_NAME = "지역구분";

	private final RestClient restClient;
	private final PoliceCommonCodeXmlParser parser;
	private final String serviceKey;
	private final int numOfRows;

	public DataGoKrPoliceCommonCodeClient(
			@Value("${police.common-code.base-url}") String baseUrl,
			@Value("${police.common-code.service-key:}") String serviceKey,
			@Value("${police.common-code.num-of-rows:500}") int numOfRows,
			PoliceCommonCodeXmlParser parser
	) {
		this.restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.build();
		this.serviceKey = serviceKey;
		this.numOfRows = numOfRows;
		this.parser = parser;
	}

	@Override
	public List<PoliceRegionCode> fetchRegionCodes() {
		if (!StringUtils.hasText(serviceKey)) {
			throw new IllegalStateException("경찰청 공통코드 API serviceKey가 설정되지 않았습니다.");
		}

		String response = utf8(restClient.get()
				.uri(uriBuilder -> uriBuilder
						.queryParam("serviceKey", serviceKey)
						.queryParam("GRP_NM", REGION_GROUP_NAME)
						.queryParam("pageNo", 1)
						.queryParam("numOfRows", numOfRows)
						.build())
				.retrieve()
				.body(byte[].class));

		String resultCode = parser.resultCode(response);
		if (StringUtils.hasText(resultCode) && !"00".equals(resultCode)) {
			throw new IllegalStateException("경찰청 공통코드 API 호출 실패: " + resultCode + " " + parser.resultMessage(response));
		}
		return parser.parseRegionCodes(response).stream()
				.sorted(Comparator.comparing(PoliceRegionCode::code))
				.toList();
	}

	private String utf8(byte[] response) {
		if (response == null || response.length == 0) {
			return "";
		}
		return new String(response, StandardCharsets.UTF_8);
	}
}
